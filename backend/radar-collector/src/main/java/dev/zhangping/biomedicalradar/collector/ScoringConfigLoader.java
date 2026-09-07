package dev.zhangping.biomedicalradar.collector;

import dev.zhangping.biomedicalradar.domain.ImportanceScorer;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ScoringConfigLoader {
    @SuppressWarnings("unchecked")
    public ImportanceScorer.Policy load(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(input);
            if (!(loaded instanceof Map<?, ?> root) || !(root.get("importance") instanceof Map<?, ?> raw)) {
                throw new IOException("scoring.yml must contain importance settings");
            }
            Map<String, Object> values = (Map<String, Object>) raw;
            List<Object> eventValues = values.get("highValueEvents") instanceof List<?> list
                    ? (List<Object>) list : List.of();
            Set<String> events = eventValues.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
            return new ImportanceScorer.Policy(
                    number(values, "tierA"), number(values, "tierB"), number(values, "tierC"),
                    number(values, "highValueEvent"), number(values, "highValueEventMaximum"),
                    number(values, "policyOrSafety"), number(values, "publishedWithin24Hours"),
                    number(values, "publishedWithin72Hours"), number(values, "additionalSource"),
                    number(values, "additionalSourceMaximum"), number(values, "mustReadMinimum"),
                    number(values, "tierCMaximum"), events);
        }
    }

    private int number(Map<String, Object> values, String key) throws IOException {
        Object value = values.get(key);
        if (!(value instanceof Number number)) {
            throw new IOException("Missing numeric score setting: " + key);
        }
        return number.intValue();
    }
}
