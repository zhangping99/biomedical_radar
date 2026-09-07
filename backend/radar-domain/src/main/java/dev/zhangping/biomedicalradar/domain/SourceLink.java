package dev.zhangping.biomedicalradar.domain;

import static dev.zhangping.biomedicalradar.domain.DomainEnums.SourceTier;

public record SourceLink(
        String sourceId,
        String sourceName,
        SourceTier sourceTier,
        String originalUrl
) {
}
