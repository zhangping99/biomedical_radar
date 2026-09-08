package dev.zhangping.biomedicalradar.collector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConnectorRegistry {
    private final Map<String, SourceConnector> connectors;

    public ConnectorRegistry() {
        List<SourceConnector> available = List.of(
                new RssConnector(), new ClinicalTrialsConnector(), new EuropePmcConnector(),
                new PubMedConnector(), new AmgenNewsConnector(), new SecSubmissionsConnector(),
                new HtmlListConnector());
        Map<String, SourceConnector> indexed = new LinkedHashMap<>();
        available.forEach(connector -> indexed.put(connector.type(), connector));
        connectors = Map.copyOf(indexed);
    }

    private static final class AmgenNewsConnector implements SourceConnector {
        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM.dd.uuuu");

        @Override
        public String type() {
            return "amgen";
        }

        @Override
        public List<RawSourceItem> collect(SourceDefinition source, FetchClient fetchClient) throws Exception {
            JsonArray months = JsonParser.parseString(fetchClient.fetch(source)).getAsJsonArray();
            List<RawSourceItem> result = new ArrayList<>();
            int maximumItems = maxItems(source, 20);
            for (JsonElement monthElement : months) {
                JsonArray releases = array(monthElement.getAsJsonObject(), "NewsReleases");
                for (JsonElement releaseElement : releases) {
                    JsonObject release = releaseElement.getAsJsonObject();
                    String title = string(release, "Title");
                    String href = string(release, "Url");
                    if (title.isBlank() || href.isBlank()) {
                        continue;
                    }
                    String url = URI.create(source.url()).resolve(href).toString();
                    result.add(new RawSourceItem(url, url, title, "Official Amgen news release",
                            parseDate(string(release, "Date"), DATE_FORMAT), List.of("Amgen"), List.of()));
                    if (result.size() >= maximumItems) {
                        return result;
                    }
                }
            }
            return result;
        }
    }

    public SourceConnector require(String type) {
        SourceConnector connector = connectors.get(type);
        if (connector == null) {
            throw new IllegalArgumentException("Unsupported connector: " + type);
        }
        return connector;
    }

    private static final class RssConnector implements SourceConnector {
        @Override
        public String type() {
            return "rss";
        }

        @Override
        public List<RawSourceItem> collect(SourceDefinition source, FetchClient fetchClient) throws Exception {
            String payload = fetchClient.fetch(source);
            try (XmlReader reader = new XmlReader(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)))) {
                List<RawSourceItem> result = new ArrayList<>();
                int maximumItems = maxItems(source, 30);
                for (SyndEntry entry : new SyndFeedInput().build(reader).getEntries()) {
                    String link = firstNonBlank(entry.getLink(), entry.getUri());
                    String description = entry.getDescription() == null ? null
                            : Jsoup.parse(entry.getDescription().getValue()).text();
                    Instant date = entry.getPublishedDate() == null ? null : entry.getPublishedDate().toInstant();
                    result.add(new RawSourceItem(entry.getUri(), link, entry.getTitle(), description, date,
                            List.of(), List.of()));
                    if (result.size() >= maximumItems) {
                        break;
                    }
                }
                return result;
            }
        }
    }

    private static final class ClinicalTrialsConnector implements SourceConnector {
        @Override
        public String type() {
            return "clinicaltrials";
        }

        @Override
        public List<RawSourceItem> collect(SourceDefinition source, FetchClient fetchClient) throws Exception {
            JsonArray studies = array(object(JsonParser.parseString(fetchClient.fetch(source))), "studies");
            List<RawSourceItem> result = new ArrayList<>();
            int maximumItems = maxItems(source, 20);
            for (JsonElement element : studies) {
                JsonObject protocol = object(element.getAsJsonObject(), "protocolSection");
                JsonObject identification = object(protocol, "identificationModule");
                JsonObject status = object(protocol, "statusModule");
                JsonObject conditions = object(protocol, "conditionsModule");
                String nctId = string(identification, "nctId");
                List<String> diseaseAreas = strings(array(conditions, "conditions"));
                result.add(new RawSourceItem(nctId,
                        "https://clinicaltrials.gov/study/" + nctId,
                        string(identification, "briefTitle"),
                        "Clinical study registration: " + String.join(", ", diseaseAreas),
                        parseDate(firstNonBlank(string(status, "studyFirstPostDateStruct.date"),
                                string(status, "studyFirstPostDate"))), List.of(), diseaseAreas));
                if (result.size() >= maximumItems) {
                    break;
                }
            }
            return result;
        }
    }

    private static final class EuropePmcConnector implements SourceConnector {
        @Override
        public String type() {
            return "europepmc";
        }

        @Override
        public List<RawSourceItem> collect(SourceDefinition source, FetchClient fetchClient) throws Exception {
            JsonObject root = object(JsonParser.parseString(fetchClient.fetch(source)));
            JsonArray rows = array(object(root, "resultList"), "result");
            List<RawSourceItem> result = new ArrayList<>();
            int maximumItems = maxItems(source, 20);
            for (JsonElement element : rows) {
                JsonObject row = element.getAsJsonObject();
                String id = firstNonBlank(string(row, "pmcid"), string(row, "pmid"), string(row, "id"));
                String sourceName = string(row, "source");
                String url = "https://europepmc.org/article/" + (sourceName.isBlank() ? "MED" : sourceName) + "/" + id;
                result.add(new RawSourceItem(id, url, string(row, "title"),
                        firstNonBlank(string(row, "authorString"), string(row, "journalTitle")),
                        parseDate(firstNonBlank(string(row, "firstPublicationDate"), string(row, "journalInfo.printPublicationDate"))),
                        List.of(), List.of()));
                if (result.size() >= maximumItems) {
                    break;
                }
            }
            return result;
        }
    }

    private static final class PubMedConnector implements SourceConnector {
        @Override
        public String type() {
            return "pubmed";
        }

        @Override
        public List<RawSourceItem> collect(SourceDefinition source, FetchClient fetchClient) throws Exception {
            JsonObject searchRoot = object(JsonParser.parseString(fetchClient.fetch(source)));
            List<String> ids = strings(array(object(searchRoot, "esearchresult"), "idlist"));
            if (ids.isEmpty()) {
                return List.of();
            }
            int maximumItems = maxItems(source, 20);
            ids = ids.stream().limit(maximumItems).toList();
            String summaryTemplate = source.options().getOrDefault("summaryUrl",
                    "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=json&id={ids}");
            URI summaryUri = URI.create(summaryTemplate.replace("{ids}", String.join(",", ids)));
            URI searchUri = URI.create(source.url());
            if (!Objects.equals(searchUri.getHost(), summaryUri.getHost())) {
                throw new IllegalArgumentException("PUBMED_SUMMARY_HOST_MISMATCH");
            }
            JsonObject summaryRoot = object(JsonParser.parseString(fetchClient.fetch(source, summaryUri)));
            JsonObject results = object(summaryRoot, "result");
            List<String> orderedIds = strings(array(results, "uids"));
            if (orderedIds.isEmpty()) {
                orderedIds = ids;
            }
            List<RawSourceItem> items = new ArrayList<>();
            for (String id : orderedIds) {
                JsonObject row = object(results, id);
                String title = string(row, "title");
                if (title.isBlank()) {
                    continue;
                }
                items.add(new RawSourceItem(id, "https://pubmed.ncbi.nlm.nih.gov/" + id + "/", title,
                        firstNonBlank(string(row, "fulljournalname"), string(row, "source")),
                        parseDate(firstNonBlank(string(row, "sortpubdate"), string(row, "pubdate"))),
                        List.of(), List.of()));
                if (items.size() >= maximumItems) {
                    break;
                }
            }
            return items;
        }
    }

    private static final class SecSubmissionsConnector implements SourceConnector {
        @Override
        public String type() {
            return "sec";
        }

        @Override
        public List<RawSourceItem> collect(SourceDefinition source, FetchClient fetchClient) throws Exception {
            JsonObject root = object(JsonParser.parseString(fetchClient.fetch(source)));
            JsonObject recent = object(object(root, "filings"), "recent");
            JsonArray accession = array(recent, "accessionNumber");
            JsonArray forms = array(recent, "form");
            JsonArray documents = array(recent, "primaryDocument");
            JsonArray dates = array(recent, "filingDate");
            String company = string(root, "name");
            String cik = string(root, "cik").replaceFirst("^0+", "");
            Set<String> acceptedForms = Set.of("8-K", "10-Q", "10-K", "6-K", "20-F");
            List<RawSourceItem> result = new ArrayList<>();
            int maximumItems = maxItems(source, 20);
            int count = Math.min(Math.min(accession.size(), forms.size()), Math.min(documents.size(), dates.size()));
            for (int index = 0; index < count; index++) {
                String form = forms.get(index).getAsString();
                if (!acceptedForms.contains(form)) {
                    continue;
                }
                String accessionNumber = accession.get(index).getAsString();
                String url = "https://www.sec.gov/Archives/edgar/data/" + cik + "/"
                        + accessionNumber.replace("-", "") + "/" + documents.get(index).getAsString();
                result.add(new RawSourceItem(accessionNumber, url, company + " filed " + form,
                        "Official SEC filing " + form, parseDate(dates.get(index).getAsString()),
                        List.of(company), List.of()));
                if (result.size() >= maximumItems) {
                    break;
                }
            }
            return result;
        }
    }

    private static final class HtmlListConnector implements SourceConnector {
        @Override
        public String type() {
            return "html";
        }

        @Override
        public List<RawSourceItem> collect(SourceDefinition source, FetchClient fetchClient) throws Exception {
            String payload = fetchClient.fetch(source);
            if (Boolean.parseBoolean(source.options().getOrDefault("unwrapCdata", "false"))) {
                payload = cdataFragments(payload);
            }
            Document document = Jsoup.parse(payload, source.url());
            String itemSelector = source.options().getOrDefault("itemSelector", "a");
            String titleSelector = source.options().getOrDefault("titleSelector", "a");
            String linkSelector = source.options().getOrDefault("linkSelector", titleSelector);
            String dateSelector = source.options().getOrDefault("dateSelector", "time");
            String descriptionSelector = source.options().get("descriptionSelector");
            String titleAttribute = source.options().get("titleAttribute");
            boolean dateFromUrl = Boolean.parseBoolean(source.options().getOrDefault("dateFromUrl", "false"));
            int maximumItems = maxItems(source, 30);
            List<RawSourceItem> result = new ArrayList<>();
            for (Element item : document.select(itemSelector)) {
                Element titleElement = "self".equals(titleSelector) ? item : item.selectFirst(titleSelector);
                Element link = "self".equals(linkSelector) ? item : item.selectFirst(linkSelector);
                if (titleElement == null || link == null) {
                    continue;
                }
                String title = titleAttribute == null ? titleElement.text() : titleElement.attr(titleAttribute);
                if (title.isBlank()) {
                    title = titleElement.text();
                }
                if (title.isBlank()) {
                    continue;
                }
                String href = link.absUrl("href");
                if (href.isBlank()) {
                    href = URI.create(source.url()).resolve(link.attr("href")).toString();
                }
                if (!href.startsWith("https://") && !href.startsWith("http://")) {
                    continue;
                }
                Element date = item.selectFirst(dateSelector);
                Element description = descriptionSelector == null ? null : item.selectFirst(descriptionSelector);
                Instant publishedAt = date == null ? null : parseDate(date.text());
                if (publishedAt == null && dateFromUrl) {
                    publishedAt = parseDate(href);
                }
                result.add(new RawSourceItem(href, href, title,
                        description == null ? null : description.text(), publishedAt, List.of(), List.of()));
                if (result.size() >= maximumItems) {
                    break;
                }
            }
            return result;
        }
    }

    private static JsonObject object(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static JsonObject object(JsonObject object, String key) {
        if (key.contains(".")) {
            JsonElement current = object;
            for (String part : key.split("\\.")) {
                current = current != null && current.isJsonObject() ? current.getAsJsonObject().get(part) : null;
            }
            return ConnectorRegistry.object(current);
        }
        return object(object.get(key));
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
    }

    private static String string(JsonObject object, String key) {
        if (key.contains(".")) {
            String[] parts = key.split("\\.");
            JsonElement current = object;
            for (String part : parts) {
                current = current != null && current.isJsonObject() ? current.getAsJsonObject().get(part) : null;
            }
            return current == null || current.isJsonNull() ? "" : current.getAsString();
        }
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static List<String> strings(JsonArray values) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.getAsString()));
        return result;
    }

    private static String firstNonBlank(String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).filter(value -> !value.isBlank()).findFirst().orElse("");
    }

    private static Instant parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            for (DatePattern pattern : DATE_PATTERNS) {
                Matcher matcher = pattern.pattern().matcher(value);
                if (matcher.find()) {
                    try {
                        return LocalDate.parse(matcher.group(), pattern.formatter())
                                .atStartOfDay().toInstant(ZoneOffset.UTC);
                    } catch (DateTimeParseException invalid) {
                        // Try the next supported public date shape.
                    }
                }
            }
            return null;
        }
    }

    private static Instant parseDate(String value, DateTimeFormatter formatter) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, formatter).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException invalid) {
            return null;
        }
    }

    private static int maxItems(SourceDefinition source, int fallback) {
        String configured = source.options().get("maxItems");
        if (configured == null || configured.isBlank()) {
            return fallback;
        }
        int parsed = Integer.parseInt(configured);
        if (parsed < 1 || parsed > 100) {
            throw new IllegalArgumentException("maxItems must be between 1 and 100");
        }
        return parsed;
    }

    private static String cdataFragments(String payload) {
        Matcher matcher = Pattern.compile("<!\\[CDATA\\[([\\s\\S]*?)]]>").matcher(payload);
        StringBuilder fragments = new StringBuilder();
        while (matcher.find()) {
            fragments.append(matcher.group(1)).append('\n');
        }
        return fragments.isEmpty() ? payload : fragments.toString();
    }

    private static final List<DatePattern> DATE_PATTERNS = List.of(
            new DatePattern(Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}"),
                    DateTimeFormatter.ofPattern("uuuu-M-d")),
            new DatePattern(Pattern.compile("\\d{4}/\\d{1,2}/\\d{1,2}"),
                    DateTimeFormatter.ofPattern("uuuu/M/d")),
            new DatePattern(Pattern.compile("\\d{4}\\.\\d{1,2}\\.\\d{1,2}"),
                    DateTimeFormatter.ofPattern("uuuu.M.d")),
            new DatePattern(Pattern.compile("\\d{1,2}\\.\\d{1,2}\\.\\d{4}"),
                    DateTimeFormatter.ofPattern("M.d.uuuu")),
            new DatePattern(Pattern.compile("\\d{4}年\\d{1,2}月\\d{1,2}日"),
                    DateTimeFormatter.ofPattern("uuuu年M月d日")),
            new DatePattern(Pattern.compile("[A-Za-z]+ \\d{1,2}, \\d{4}"),
                    DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH)),
            new DatePattern(Pattern.compile("\\d{1,2} [A-Za-z]+ \\d{4}"),
                    DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH)));

    private record DatePattern(Pattern pattern, DateTimeFormatter formatter) {
    }
}
