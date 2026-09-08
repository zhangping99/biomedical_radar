package dev.zhangping.biomedicalradar.collector;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dev.zhangping.biomedicalradar.domain.ImportanceScorer;

@SpringBootApplication
public class RadarCollectorApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(RadarCollectorApplication.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run(args);
    }

    @Bean
    CommandLineRunner runCollector() {
        return args -> {
            Map<String, String> options = parseOptions(args);
            Path sourcesFile = Path.of(options.getOrDefault("sources", "../config/sources.yml"));
            Path scoringFile = Path.of(options.getOrDefault("scoring", "../config/scoring.yml"));
            Path outputDirectory = Path.of(options.getOrDefault("output-dir", "../frontend/public/data"));
            int retentionDays = Integer.parseInt(options.getOrDefault("retention-days", "90"));
            int maxConcurrency = Integer.parseInt(options.getOrDefault("max-concurrency", "4"));
            FetchClient fetchClient = options.containsKey("fixture-dir")
                    ? new FixtureFetchClient(Path.of(options.get("fixture-dir")))
                    : new HttpFetchClient();

            List<SourceDefinition> allSources = new SourceConfigLoader().load(sourcesFile);
            List<SourceDefinition> sources = selectSources(allSources, options.getOrDefault("groups", "all"));
            NoKeyProviders providers = new NoKeyProviders();
            CollectorService collector = new CollectorService(new ConnectorRegistry(), providers, providers,
                    new ContentClassifier(), new ImportanceScorer(new ScoringConfigLoader().load(scoringFile)),
                    Clock.systemUTC());
            CollectionResult result = collector.collect(sources, fetchClient, maxConcurrency);
            Set<String> enabledSourceIds = allSources.stream().filter(SourceDefinition::enabled)
                    .map(SourceDefinition::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
            StaticExporter.ExportResult export = new StaticExporter().export(result, outputDirectory, retentionDays,
                    enabledSourceIds);
            long failedSources = result.sourceHealth().stream().filter(source -> "failed".equals(source.status())).count();
            System.out.printf("Exported %d articles to %s (%d files, %d/%d selected sources failed)%n",
                    export.articleCount(), export.outputDirectory(), export.fileCount(), failedSources, sources.size());
            if (failedSources == sources.size()) {
                throw new IllegalStateException("All selected sources failed");
            }
        };
    }

    static List<SourceDefinition> selectSources(List<SourceDefinition> definitions, String groupOption) {
        List<SourceDefinition> enabled = definitions.stream().filter(SourceDefinition::enabled).toList();
        String normalized = groupOption == null ? "all" : groupOption.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || "all".equals(normalized)) {
            if (enabled.isEmpty()) {
                throw new IllegalArgumentException("No enabled sources are configured");
            }
            return enabled;
        }

        Set<String> requested = new LinkedHashSet<>();
        for (String group : normalized.split(",")) {
            if (!group.isBlank()) {
                requested.add(group.trim());
            }
        }
        Set<String> available = enabled.stream().map(SourceDefinition::scheduleGroup)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> unknown = new LinkedHashSet<>(requested);
        unknown.removeAll(available);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown source groups: " + String.join(", ", unknown));
        }
        List<SourceDefinition> selected = enabled.stream()
                .filter(source -> requested.contains(source.scheduleGroup()))
                .toList();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("No enabled sources selected for groups: " + groupOption);
        }
        return selected;
    }

    private Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String argument : args) {
            if (!argument.startsWith("--") || !argument.contains("=")) {
                continue;
            }
            String[] parts = argument.substring(2).split("=", 2);
            options.put(parts[0], parts[1]);
        }
        return options;
    }
}
