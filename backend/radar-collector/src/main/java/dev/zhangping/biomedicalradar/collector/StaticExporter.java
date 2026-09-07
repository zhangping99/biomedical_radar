package dev.zhangping.biomedicalradar.collector;

import com.google.gson.Gson;
import dev.zhangping.biomedicalradar.domain.Article;
import dev.zhangping.biomedicalradar.domain.ArticleDeduplicator;
import dev.zhangping.biomedicalradar.domain.DomainEnums;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class StaticExporter {
    public static final String SCHEMA_VERSION = "1.0";
    private final Gson gson = JsonSupport.gson();

    public ExportResult export(CollectionResult result, Path outputDirectory, int retentionDays) throws IOException {
        Path output = outputDirectory.toAbsolutePath().normalize();
        Files.createDirectories(output);
        Instant generatedAt = result.completedAt();
        List<Article> retained = retainAndMerge(result.articles(), output.resolve("latest.json"), generatedAt, retentionDays);

        Map<String, FileMetadata> files = new LinkedHashMap<>();
        write(output, "latest.json", new FeedDocument(SCHEMA_VERSION, generatedAt, retained), files);

        Map<LocalDate, List<Article>> byDate = new TreeMap<>(Comparator.reverseOrder());
        for (Article article : retained) {
            Instant timestamp = article.publishedAt() == null ? article.collectedAt() : article.publishedAt();
            byDate.computeIfAbsent(timestamp.atZone(ZoneOffset.UTC).toLocalDate(), ignored -> new ArrayList<>()).add(article);
        }
        for (Map.Entry<LocalDate, List<Article>> entry : byDate.entrySet()) {
            write(output, "daily/" + entry.getKey() + ".json",
                    new FeedDocument(SCHEMA_VERSION, generatedAt, entry.getValue()), files);
        }
        removeUnreferencedDailyFiles(output.resolve("daily"), byDate.keySet());

        TaxonomyDocument taxonomy = new TaxonomyDocument(SCHEMA_VERSION, generatedAt,
                names(DomainEnums.Category.values()), names(DomainEnums.Region.values()),
                names(DomainEnums.SourceTier.values()));
        write(output, "taxonomy.json", taxonomy, files);
        write(output, "source-health.json",
                new SourceHealthDocument(SCHEMA_VERSION, generatedAt, result.startedAt(), result.completedAt(),
                        result.sourceHealth()), files);

        String latestDate = byDate.isEmpty() ? generatedAt.atZone(ZoneOffset.UTC).toLocalDate().toString()
                : byDate.keySet().iterator().next().toString();
        FeedManifest manifest = new FeedManifest(SCHEMA_VERSION, generatedAt, latestDate,
                byDate.keySet().stream().map(LocalDate::toString).toList(), files,
                freshness(generatedAt, result.sourceHealth()));
        write(output, "feed-manifest.json", manifest, null);
        return new ExportResult(output, retained.size(), files.size() + 1);
    }

    private List<Article> retainAndMerge(List<Article> current, Path existingLatest, Instant now, int retentionDays) {
        List<Article> combined = new ArrayList<>(current);
        if (Files.isRegularFile(existingLatest)) {
            try {
                FeedDocument existing = gson.fromJson(Files.readString(existingLatest), FeedDocument.class);
                if (existing != null && SCHEMA_VERSION.equals(existing.schemaVersion()) && existing.articles() != null) {
                    combined.addAll(existing.articles());
                }
            } catch (Exception ignored) {
                // A malformed previous export must not be republished; the new verified batch remains usable.
            }
        }
        Instant cutoff = now.minus(Math.max(1, retentionDays), ChronoUnit.DAYS);
        List<Article> retained = combined.stream().filter(article -> {
                    Instant time = article.publishedAt() == null ? article.collectedAt() : article.publishedAt();
                    return time != null && !time.isBefore(cutoff);
                }).toList();
        return new ArticleDeduplicator().deduplicate(retained).articles();
    }

    private void write(Path root, String relativePath, Object value, Map<String, FileMetadata> manifestFiles)
            throws IOException {
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Output path escapes export root: " + relativePath);
        }
        Files.createDirectories(target.getParent());
        byte[] bytes = (gson.toJson(value) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        Files.write(target, bytes);
        if (manifestFiles != null) {
            manifestFiles.put(relativePath.replace('\\', '/'), new FileMetadata(sha256(bytes), bytes.length));
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private void removeUnreferencedDailyFiles(Path dailyDirectory, Set<LocalDate> retainedDates) throws IOException {
        if (!Files.isDirectory(dailyDirectory)) {
            return;
        }
        Set<String> retainedNames = retainedDates.stream()
                .map(date -> date + ".json")
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        try (var files = Files.list(dailyDirectory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                if (file.getFileName().toString().matches("\\d{4}-\\d{2}-\\d{2}\\.json")
                        && !retainedNames.contains(file.getFileName().toString())) {
                    Files.delete(file);
                }
            }
        }
    }

    private String freshness(Instant generatedAt, List<SourceHealth> health) {
        boolean anySuccess = health.stream().anyMatch(source -> "ok".equals(source.status()));
        boolean anyFailure = health.stream().anyMatch(source -> "failed".equals(source.status()));
        if (!anySuccess) {
            return "failed";
        }
        if (anyFailure) {
            return "partial";
        }
        return generatedAt.isBefore(Instant.now().minus(12, ChronoUnit.HOURS)) ? "stale" : "fresh";
    }

    private List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    public record FeedDocument(String schemaVersion, Instant generatedAt, List<Article> articles) {
    }

    public record FileMetadata(String sha256, long sizeBytes) {
    }

    public record FeedManifest(String schemaVersion, Instant generatedAt, String latestDate,
                               List<String> availableDates, Map<String, FileMetadata> files, String freshness) {
    }

    public record TaxonomyDocument(String schemaVersion, Instant generatedAt, List<String> categories,
                                   List<String> regions, List<String> sourceTiers) {
    }

    public record SourceHealthDocument(String schemaVersion, Instant generatedAt, Instant batchStartedAt,
                                       Instant batchCompletedAt, List<SourceHealth> sources) {
    }

    public record ExportResult(Path outputDirectory, int articleCount, int fileCount) {
    }
}
