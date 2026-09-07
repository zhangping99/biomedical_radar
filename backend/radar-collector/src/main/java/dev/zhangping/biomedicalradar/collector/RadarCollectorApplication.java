package dev.zhangping.biomedicalradar.collector;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            FetchClient fetchClient = options.containsKey("fixture-dir")
                    ? new FixtureFetchClient(Path.of(options.get("fixture-dir")))
                    : new HttpFetchClient();

            List<SourceDefinition> sources = new SourceConfigLoader().load(sourcesFile);
            NoKeyProviders providers = new NoKeyProviders();
            CollectorService collector = new CollectorService(new ConnectorRegistry(), providers, providers,
                    new ContentClassifier(), new ImportanceScorer(new ScoringConfigLoader().load(scoringFile)),
                    Clock.systemUTC());
            CollectionResult result = collector.collect(sources, fetchClient);
            StaticExporter.ExportResult export = new StaticExporter().export(result, outputDirectory, retentionDays);
            long failedSources = result.sourceHealth().stream().filter(source -> "failed".equals(source.status())).count();
            System.out.printf("Exported %d articles to %s (%d files, %d failed sources)%n",
                    export.articleCount(), export.outputDirectory(), export.fileCount(), failedSources);
            if (failedSources == sources.stream().filter(SourceDefinition::enabled).count()) {
                throw new IllegalStateException("All enabled sources failed");
            }
        };
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
