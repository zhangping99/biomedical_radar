package dev.zhangping.biomedicalradar.collector;

import dev.zhangping.biomedicalradar.domain.*;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
        return collect(definitions, fetchClient, 4);
    }

    public CollectionResult collect(List<SourceDefinition> definitions, FetchClient fetchClient, int maxConcurrency) {
        if (maxConcurrency < 1 || maxConcurrency > 8) {
            throw new IllegalArgumentException("maxConcurrency must be between 1 and 8");
        }
        Instant batchStart = clock.instant();
        List<SourceDefinition> enabledSources = definitions.stream().filter(SourceDefinition::enabled).toList();
        List<Article> collected = new ArrayList<>();
        List<SourceHealth> health = new ArrayList<>();

        if (!enabledSources.isEmpty()) {
            int poolSize = Math.min(maxConcurrency, enabledSources.size());
            ExecutorService executor = Executors.newFixedThreadPool(poolSize);
            try {
                List<Future<SourceCollection>> futures = enabledSources.stream()
                        .map(source -> executor.submit(() -> collectSource(source, fetchClient)))
                        .toList();
                for (Future<SourceCollection> future : futures) {
                    SourceCollection sourceCollection = get(future);
                    collected.addAll(sourceCollection.articles());
                    health.add(sourceCollection.health());
                }
            } finally {
                executor.shutdownNow();
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

    private SourceCollection collectSource(SourceDefinition source, FetchClient fetchClient) {
        Instant sourceStart = clock.instant();
        try {
            List<RawSourceItem> rawItems = connectors.require(source.connector()).collect(source, fetchClient);
            int minimumItems = intOption(source, "minimumItems", 1, 0, 100);
            if (rawItems.size() < minimumItems) {
                throw new IllegalStateException("NO_ITEMS");
            }
            List<Article> sourceArticles = rawItems.stream()
                    .filter(item -> item.title() != null && !item.title().isBlank())
                    .filter(item -> item.originalUrl() != null && !item.originalUrl().isBlank())
                    .map(item -> map(source, item))
                    .toList();
            ArticleDeduplicator.Result sourceResult = new ArticleDeduplicator().deduplicate(sourceArticles);
            SourceHealth sourceHealth = new SourceHealth(source.id(), source.name(), "ok", sourceStart, clock.instant(),
                    rawItems.size(), sourceResult.articles().size(), sourceResult.duplicateCount(),
                    rawItems.size() - sourceArticles.size(), java.util.Map.of("2xx", 1), null, null);
            return new SourceCollection(sourceResult.articles(), sourceHealth);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String errorCode = safeErrorCode(exception);
            SourceHealth sourceHealth = new SourceHealth(source.id(), source.name(), "failed", sourceStart,
                    clock.instant(), 0, 0, 0, 1, statusClass(errorCode), errorCode,
                    clock.instant().plus(retryDelayHours(source), ChronoUnit.HOURS));
            return new SourceCollection(List.of(), sourceHealth);
        }
    }

    private SourceCollection get(Future<SourceCollection> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Collector interrupted", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unexpected collector task failure", exception.getCause());
        }
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
        List<String> entityNames = new ArrayList<>(raw.entityNames());
        String configuredEntity = source.options().get("entityName");
        if (configuredEntity != null && !configuredEntity.isBlank() && !entityNames.contains(configuredEntity)) {
            entityNames.add(configuredEntity);
        }
        EntityType entityType = entityType(source);
        List<EntityRef> entities = entityNames.stream()
                .map(name -> new EntityRef(entityType, name))
                .toList();
        VerificationStatus verificationStatus = enumOption(source, "verificationStatus",
                VerificationStatus.class, VerificationStatus.auto_checked);
        return new Article(
                ArticleIdentity.stableId(source.id(), raw.sourceUniqueKey(), canonicalUrl),
                source.id(), source.name(), source.tier(), source.type(), raw.originalUrl(), canonicalUrl,
                title, translation.text(), summary.text(), null, source.language(), source.region(),
                classifier.category(source, raw), events, entities, raw.diseaseAreas(), publishedAt, collectedAt,
                ArticleIdentity.contentHash(canonicalUrl, title, raw.description()), generated, generatorVersion, generatedAt,
                verificationStatus, OriginalAccessStatus.reachable, source.legalBasis(),
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
        if (message != null && message.matches("(?:HTTP_\\d{3}|[A-Z][A-Z0-9_]{2,99})")) {
            return message;
        }
        return exception.getClass().getSimpleName().toUpperCase(java.util.Locale.ROOT);
    }

    private long retryDelayHours(SourceDefinition source) {
        return switch (source.scheduleGroup()) {
            case "rapid" -> 4;
            case "policy", "research" -> 12;
            case "institutional" -> 24;
            default -> 4;
        };
    }

    private EntityType entityType(SourceDefinition source) {
        EntityType fallback = source.type() == SourceType.exchange || source.type() == SourceType.company
                ? EntityType.company : EntityType.other;
        return enumOption(source, "entityType", EntityType.class, fallback);
    }

    private int intOption(SourceDefinition source, String key, int fallback, int minimum, int maximum) {
        String value = source.options().get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        int parsed = Integer.parseInt(value);
        if (parsed < minimum || parsed > maximum) {
            throw new IllegalArgumentException(key + " must be between " + minimum + " and " + maximum);
        }
        return parsed;
    }

    private <T extends Enum<T>> T enumOption(SourceDefinition source, String key, Class<T> type, T fallback) {
        String value = source.options().get(key);
        return value == null || value.isBlank() ? fallback : Enum.valueOf(type, value);
    }

    private java.util.Map<String, Integer> statusClass(String code) {
        if (code.startsWith("HTTP_") && code.length() >= 6) {
            return java.util.Map.of(code.substring(5, 6) + "xx", 1);
        }
        return java.util.Map.of();
    }

    private record SourceCollection(List<Article> articles, SourceHealth health) {
    }
}
