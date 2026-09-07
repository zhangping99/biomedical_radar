package dev.zhangping.biomedicalradar.domain;

import java.time.Instant;
import java.util.List;

import static dev.zhangping.biomedicalradar.domain.DomainEnums.*;

public record Article(
        String id,
        String sourceId,
        String sourceName,
        SourceTier sourceTier,
        SourceType sourceType,
        String originalUrl,
        String canonicalUrl,
        String titleOriginal,
        String titleZh,
        String summaryZh,
        String whyItMattersZh,
        String language,
        Region region,
        Category category,
        List<String> eventTypes,
        List<EntityRef> entities,
        List<String> diseaseAreas,
        Instant publishedAt,
        Instant collectedAt,
        String contentHash,
        boolean generated,
        String generatorVersion,
        Instant generatedAt,
        VerificationStatus verificationStatus,
        OriginalAccessStatus originalAccessStatus,
        LegalBasis legalBasis,
        List<SourceLink> sourceLinks,
        int importanceScore,
        List<String> importanceReasons
) {
    public Article {
        eventTypes = List.copyOf(eventTypes == null ? List.of() : eventTypes);
        entities = List.copyOf(entities == null ? List.of() : entities);
        diseaseAreas = List.copyOf(diseaseAreas == null ? List.of() : diseaseAreas);
        sourceLinks = List.copyOf(sourceLinks == null ? List.of() : sourceLinks);
        importanceReasons = List.copyOf(importanceReasons == null ? List.of() : importanceReasons);
    }
}
