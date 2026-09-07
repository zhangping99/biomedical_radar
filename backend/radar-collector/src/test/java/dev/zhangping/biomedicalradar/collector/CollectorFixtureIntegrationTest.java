package dev.zhangping.biomedicalradar.collector;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CollectorFixtureIntegrationTest {
    private static final Path REPOSITORY_ROOT = Path.of("../..").toAbsolutePath().normalize();

    @Test
    void allSixConfiguredSourcesProduceFixtureArticlesAndStaticExport(@TempDir Path output) throws Exception {
        Files.createDirectories(output.resolve("daily"));
        Files.writeString(output.resolve("daily/2000-01-01.json"), "stale");
        List<SourceDefinition> sources = new SourceConfigLoader().load(REPOSITORY_ROOT.resolve("config/sources.yml"));
        NoKeyProviders providers = new NoKeyProviders();
        CollectorService service = new CollectorService(new ConnectorRegistry(), providers, providers,
                new ContentClassifier(), Clock.fixed(Instant.parse("2026-09-07T00:00:00Z"), ZoneOffset.UTC));

        CollectionResult result = service.collect(sources,
                new FixtureFetchClient(REPOSITORY_ROOT.resolve("data/fixtures/sources")));
        StaticExporter.ExportResult export = new StaticExporter().export(result, output, 90);

        assertThat(sources).hasSize(6);
        assertThat(result.sourceHealth()).allMatch(health -> "ok".equals(health.status()));
        assertThat(result.articles()).hasSize(6);
        assertThat(result.articles()).allMatch(article -> article.sourceLinks().size() == 1);
        assertThat(result.articles()).filteredOn(article -> article.sourceId().equals("amgen-news-releases"))
                .singleElement().satisfies(article -> assertThat(article.entities()).singleElement()
                        .extracting(entity -> entity.type().name()).isEqualTo("company"));
        assertThat(export.fileCount()).isGreaterThanOrEqualTo(5);
        assertThat(output.resolve("feed-manifest.json")).isRegularFile();
        assertThat(output.resolve("latest.json")).isRegularFile();
        assertThat(output.resolve("source-health.json")).isRegularFile();
        assertThat(output.resolve("daily/2000-01-01.json")).doesNotExist();
        assertThat(Files.readString(output.resolve("latest.json")))
                .endsWith("\n")
                .doesNotContain("\r\n");

        JsonObject manifest = JsonParser.parseString(Files.readString(output.resolve("feed-manifest.json"))).getAsJsonObject();
        assertThat(manifest.get("schemaVersion").getAsString()).isEqualTo("1.0");
        assertThat(manifest.getAsJsonObject("files").getAsJsonObject("latest.json")
                .get("sha256").getAsString()).hasSize(64);
    }

    @Test
    void oneSourceFailureDoesNotAbortBatch() throws Exception {
        List<SourceDefinition> sources = new SourceConfigLoader().load(REPOSITORY_ROOT.resolve("config/sources.yml"));
        NoKeyProviders providers = new NoKeyProviders();
        CollectorService service = new CollectorService(new ConnectorRegistry(), providers, providers,
                new ContentClassifier(), Clock.systemUTC());
        FetchClient selectiveFailure = source -> {
            if (source.id().equals("ema-news")) {
                throw new java.io.IOException("HTTP_503");
            }
            return new FixtureFetchClient(REPOSITORY_ROOT.resolve("data/fixtures/sources")).fetch(source);
        };

        CollectionResult result = service.collect(sources, selectiveFailure);

        assertThat(result.articles()).hasSize(5);
        assertThat(result.sourceHealth()).filteredOn(health -> "failed".equals(health.status()))
                .singleElement().extracting(SourceHealth::sourceId).isEqualTo("ema-news");
    }

    @Test
    void futurePublicationDateDoesNotPolluteTheTimeline() {
        Instant now = Instant.parse("2026-09-07T00:00:00Z");
        SourceDefinition source = new SourceDefinition("future-rss", "Future RSS", true,
                dev.zhangping.biomedicalradar.domain.DomainEnums.SourceTier.A,
                dev.zhangping.biomedicalradar.domain.DomainEnums.SourceType.journal,
                dev.zhangping.biomedicalradar.domain.DomainEnums.Region.GLOBAL,
                "en", "rss", "https://example.test/feed.xml", "normal",
                dev.zhangping.biomedicalradar.domain.DomainEnums.LegalBasis.rss_allowed, 10,
                "unused.xml", java.util.Map.of());
        String feed = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel><title>Future RSS</title>
                  <item><guid>future-1</guid><title>Future-dated publication</title>
                    <link>https://example.test/future-1</link><pubDate>Mon, 28 Dec 2026 00:00:00 GMT</pubDate>
                  </item>
                </channel></rss>
                """;
        NoKeyProviders providers = new NoKeyProviders();
        CollectorService service = new CollectorService(new ConnectorRegistry(), providers, providers,
                new ContentClassifier(), Clock.fixed(now, ZoneOffset.UTC));

        CollectionResult result = service.collect(List.of(source), ignored -> feed);

        assertThat(result.articles()).singleElement().extracting(article -> article.publishedAt()).isNull();
    }
}
