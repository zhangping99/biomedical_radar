package dev.zhangping.biomedicalradar.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ArticleDeduplicator {
    public Result deduplicate(List<Article> input) {
        Map<String, Article> exact = new LinkedHashMap<>();
        int removed = 0;
        for (Article candidate : input) {
            String key = candidate.sourceId() + "|" + candidate.canonicalUrl();
            if (exact.putIfAbsent(key, candidate) != null) {
                removed++;
            }
        }

        List<Article> output = new ArrayList<>();
        for (Article candidate : exact.values()) {
            int match = findCertainCrossSourceMatch(output, candidate);
            if (match < 0) {
                output.add(candidate);
            } else {
                output.set(match, merge(output.get(match), candidate));
                removed++;
            }
        }
        return new Result(List.copyOf(output), removed);
    }

    private int findCertainCrossSourceMatch(List<Article> output, Article candidate) {
        String candidateTitle = TextNormalizer.fingerprint(candidate.titleOriginal());
        if (candidateTitle.length() < 12 || candidate.eventTypes().isEmpty()) {
            return -1;
        }
        for (int index = 0; index < output.size(); index++) {
            Article current = output.get(index);
            if (Objects.equals(current.sourceId(), candidate.sourceId())) {
                continue;
            }
            boolean sameTitle = candidateTitle.equals(TextNormalizer.fingerprint(current.titleOriginal()));
            boolean sharedEvent = current.eventTypes().stream().anyMatch(candidate.eventTypes()::contains);
            if (sameTitle && sharedEvent && withinWindow(current, candidate)) {
                return index;
            }
        }
        return -1;
    }

    private boolean withinWindow(Article first, Article second) {
        Instant a = first.publishedAt() == null ? first.collectedAt() : first.publishedAt();
        Instant b = second.publishedAt() == null ? second.collectedAt() : second.publishedAt();
        return Math.abs(Duration.between(a, b).toHours()) <= 72;
    }

    private Article merge(Article first, Article second) {
        List<SourceLink> links = new ArrayList<>(first.sourceLinks());
        second.sourceLinks().stream()
                .filter(link -> links.stream().noneMatch(existing -> existing.originalUrl().equals(link.originalUrl())))
                .forEach(links::add);
        Article primary = first.sourceTier().ordinal() <= second.sourceTier().ordinal() ? first : second;
        return new Article(primary.id(), primary.sourceId(), primary.sourceName(), primary.sourceTier(),
                primary.sourceType(), primary.originalUrl(), primary.canonicalUrl(), primary.titleOriginal(),
                primary.titleZh(), primary.summaryZh(), primary.whyItMattersZh(), primary.language(),
                primary.region(), primary.category(), primary.eventTypes(), primary.entities(),
                primary.diseaseAreas(), primary.publishedAt(), primary.collectedAt(), primary.contentHash(),
                primary.generated(), primary.generatorVersion(), primary.generatedAt(),
                primary.verificationStatus(), primary.originalAccessStatus(), primary.legalBasis(), links,
                primary.importanceScore(), primary.importanceReasons());
    }

    public record Result(List<Article> articles, int duplicateCount) {
    }
}
