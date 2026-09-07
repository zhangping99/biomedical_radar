package dev.zhangping.biomedicalradar.collector;

import dev.zhangping.biomedicalradar.domain.*;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static dev.zhangping.biomedicalradar.domain.DomainEnums.*;

public final class CollectorService {
    private final ConnectorRegistry connectors;
    private final TranslationProvider translationProvider;
    private final SummaryProvider summaryProvider;
    private final ContentClassifier classifier;
    private final ImportanceScorer importanceScorer;
    private final Clock clock;

    public CollectorService(ConnectorRegistry connectors, TranslationProvider translationProvider,
                            SummaryProvider summaryProvider, ContentClassifier classifier, Clock clock) {
        this(connectors, translationProvider, summaryProvider, classifier, new ImportanceScorer(), clock);
    }

    public CollectorService(ConnectorRegistry connectors, TranslationProvider translationProvider,
                            SummaryProvider summaryProvider, ContentClassifier classifier,
                            ImportanceScorer importanceScorer, Clock clock) {
        this.connectors = connectors;
        this.translationProvider = translationProvider;
        this.summaryProvider = summaryProvider;
        this.classifier = classifier;
        this.importanceScorer = importanceScorer;
        this.clock = clock;
    }

    public CollectionResult collect(List<SourceDefinition> definitions, FetchClient fetchClient) {
        Instant batchStart = clock.instant();
        List<Article> collected = new ArrayList<>();
        List<SourceHealth> health = new ArrayList<>();

        for (SourceDefinition source : definitions.stream().filter(SourceDefinition::enabled).toList()) {
            Instant sourceStart = clock.instant();
            try {
                List<RawSourceItem> rawItems = connectors.require(source.connector()).collect(source, fetchClient);
                List<Article> sourceArticles = rawItems.stream()
                        .filter(item -> item.title() != null && !item.title().isBlank())
                        .filter(item -> item.originalUrl() != null && !item.originalUrl().isBlank())
                        .map(item -> map(source, item))
                        .toList();
                ArticleDeduplicator.Result sourceResult = new ArticleDeduplicator().deduplicate(sourceArticles);
                collected.addAll(sourceResult.articles());
                health.add(new SourceHealth(source.id(), source.name(), "ok", sourceStart, clock.instant(),
                        rawItems.size(), sourceResult.articles().size(), sourceResult.duplicateCount(),
                        rawItems.size() - sourceArticles.size(), java.util.Map.of("2xx", 1), null, null));
            } catch (Exception exception) {
                String errorCode = safeErrorCode(exception);
                health.add(new SourceHealth(source.id(), source.name(), "failed", sourceStart, clock.instant(),
                        0, 0, 0, 1, statusClass(errorCode), errorCode,
                        clock.instant().plus(4, ChronoUnit.HOURS)));
            }
        }

        ArticleDeduplicator.Result batchResult = new ArticleDeduplicator().deduplicate(collected);
        List<Article> scored = batchResult.articles().stream().map(this::score)
                .sorted(Comparator.comparingInt(Article::importanceScore).reversed()
                        .thenComparing(article -> article.publishedAt() == null ? article.collectedAt() : article.publishedAt(),
                                Comparator.reverseOrder()))
                .toList();
        return new CollectionResult(batchStart, clock.instant(), scored, health, batchResult.duplicateCount());
    }

    private Article map(SourceDefinition source, RawSourceItem raw) {
        Instant collectedAt = clock.instant();
        Instant publishedAt = raw.publishedAt() != null && raw.publishedAt().isAfter(collectedAt.plus(1, ChronoUnit.DAYS))
                ? null
                : raw.publishedAt();
        String canonicalUrl = UrlNormalizer.normalize(raw.originalUrl());
        String title = TextNormalizer.normalize(raw.title());
        GeneratedText translation = translationProvider.translateTitle(title, source.language());
        GeneratedText summary = summaryProvider.summarize(raw.description(), source.language());
        boolean generated = translation.generated() || summary.generated();
        String generatorVersion = firstNonNull(translation.generatorVersion(), summary.generatorVersion());
        Instant generatedAt = translation.generatedAt() == null ? summary.generatedAt() : translation.generatedAt();
        List<String> events = classifier.eventTypes(raw);
        List<EntityRef> entities = raw.entityNames().stream()
                .map(name -> new EntityRef(source.type() == SourceType.exchange || source.type() == SourceType.company
                        ? EntityType.company : EntityType.other, name))
                .toList();
        return new Article(
                ArticleIdentity.stableId(source.id(), raw.sourceUniqueKey(), canonicalUrl),
                source.id(), source.name(), source.tier(), source.type(), raw.originalUrl(), canonicalUrl,
                title, translation.text(), summary.text(), null, source.language(), source.region(),
                classifier.category(source, raw), events, entities, raw.diseaseAreas(), publishedAt, collectedAt,
                ArticleIdentity.contentHash(canonicalUrl, title, raw.description()), generated, generatorVersion, generatedAt,
                VerificationStatus.auto_checked, OriginalAccessStatus.reachable, source.legalBasis(),
                List.of(new SourceLink(source.id(), source.name(), source.tier(), raw.originalUrl())), 0, List.of());
    }

    private Article score(Article article) {
        ImportanceScorer.Score score = importanceScorer.score(article, clock.instant());
        return new Article(article.id(), article.sourceId(), article.sourceName(), article.sourceTier(), article.sourceType(),
                article.originalUrl(), article.canonicalUrl(), article.titleOriginal(), article.titleZh(), article.summaryZh(),
                article.whyItMattersZh(), article.language(), article.region(), article.category(), article.eventTypes(),
                article.entities(), article.diseaseAreas(), article.publishedAt(), article.collectedAt(), article.contentHash(),
                article.generated(), article.generatorVersion(), article.generatedAt(), article.verificationStatus(),
                article.originalAccessStatus(), article.legalBasis(), article.sourceLinks(), score.value(), score.reasons());
    }

    private String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private String safeErrorCode(Exception exception) {
        String message = exception.getMessage();
        if (message != null && message.matches("HTTP_\\d{3}")) {
            return message;
        }
        return exception.getClass().getSimpleName().toUpperCase(java.util.Locale.ROOT);
    }

    private java.util.Map<String, Integer> statusClass(String code) {
        if (code.startsWith("HTTP_") && code.length() >= 6) {
            return java.util.Map.of(code.substring(5, 6) + "xx", 1);
        }
        return java.util.Map.of();
    }
}
