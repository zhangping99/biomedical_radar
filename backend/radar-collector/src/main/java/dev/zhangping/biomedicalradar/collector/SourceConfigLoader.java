package dev.zhangping.biomedicalradar.collector;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.zhangping.biomedicalradar.domain.DomainEnums.*;

public final class SourceConfigLoader {
    private static final Set<String> SCHEDULE_GROUPS =
            Set.of("rapid", "policy", "research", "institutional", "hospital", "pharma", "normal");

    @SuppressWarnings("unchecked")
    public List<SourceDefinition> load(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(input);
            if (!(loaded instanceof Map<?, ?> root) || !(root.get("sources") instanceof List<?> rows)) {
                throw new IOException("sources.yml must contain a sources list");
            }
            List<SourceDefinition> sources = new ArrayList<>();
            Set<String> sourceIds = new HashSet<>();
            for (Object row : rows) {
                Map<String, Object> values = (Map<String, Object>) row;
                Map<String, String> options = values.get("options") instanceof Map<?, ?> rawOptions
                        ? rawOptions.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()), entry -> String.valueOf(entry.getValue())))
                        : Map.of();
                SourceDefinition source = new SourceDefinition(
                        required(values, "id"), required(values, "name"),
                        Boolean.parseBoolean(String.valueOf(values.getOrDefault("enabled", true))),
                        SourceTier.valueOf(required(values, "tier")), SourceType.valueOf(required(values, "type")),
                        Region.valueOf(required(values, "region")), required(values, "language"),
                        required(values, "connector"), required(values, "url"),
                        String.valueOf(values.getOrDefault("scheduleGroup", "normal")),
                        LegalBasis.valueOf(required(values, "legalBasis")),
                        Integer.parseInt(String.valueOf(values.getOrDefault("timeoutSeconds", 20))),
                        values.get("fixture") == null ? null : String.valueOf(values.get("fixture")), options);
                validate(source, sourceIds);
                sources.add(source);
            }
            return List.copyOf(sources);
        }
    }

    private void validate(SourceDefinition source, Set<String> sourceIds) throws IOException {
        if (!sourceIds.add(source.id())) {
            throw new IOException("Duplicate source id: " + source.id());
        }
        if (!SCHEDULE_GROUPS.contains(source.scheduleGroup())) {
            throw new IOException("Unsupported schedule group for " + source.id() + ": " + source.scheduleGroup());
        }
        if (source.timeoutSeconds() < 5 || source.timeoutSeconds() > 120) {
            throw new IOException("timeoutSeconds must be between 5 and 120 for " + source.id());
        }
        URI uri;
        try {
            uri = URI.create(source.url());
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid source URL for " + source.id(), invalid);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IOException("Source URL must use HTTPS for " + source.id());
        }
    }

    private String required(Map<String, Object> values, String key) throws IOException {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IOException("Missing source property: " + key);
        }
        return String.valueOf(value);
    }
}
