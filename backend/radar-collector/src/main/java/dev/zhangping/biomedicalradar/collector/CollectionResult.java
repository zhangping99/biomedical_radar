package dev.zhangping.biomedicalradar.collector;

import dev.zhangping.biomedicalradar.domain.Article;

import java.time.Instant;
import java.util.List;

public record CollectionResult(
        Instant startedAt,
        Instant completedAt,
        List<Article> articles,
        List<SourceHealth> sourceHealth,
        int duplicateCount
) {
    public CollectionResult {
        articles = List.copyOf(articles);
        sourceHealth = List.copyOf(sourceHealth);
    }
}
