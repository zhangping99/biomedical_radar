package dev.zhangping.biomedicalradar.collector;

import java.time.Instant;
import java.util.Map;

public record SourceHealth(
        String sourceId,
        String sourceName,
        String status,
        Instant startedAt,
        Instant completedAt,
        int fetchedCount,
        int newCount,
        int duplicateCount,
        int failedCount,
        Map<String, Integer> httpStatusClasses,
        String errorCode,
        Instant nextRetryAt
) {
    public SourceHealth {
        httpStatusClasses = Map.copyOf(httpStatusClasses == null ? Map.of() : httpStatusClasses);
    }
}
