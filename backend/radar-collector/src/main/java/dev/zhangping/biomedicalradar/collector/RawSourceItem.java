package dev.zhangping.biomedicalradar.collector;

import java.time.Instant;
import java.util.List;

public record RawSourceItem(
        String sourceUniqueKey,
        String originalUrl,
        String title,
        String description,
        Instant publishedAt,
        List<String> entityNames,
        List<String> diseaseAreas
) {
    public RawSourceItem {
        entityNames = List.copyOf(entityNames == null ? List.of() : entityNames);
        diseaseAreas = List.copyOf(diseaseAreas == null ? List.of() : diseaseAreas);
    }
}
