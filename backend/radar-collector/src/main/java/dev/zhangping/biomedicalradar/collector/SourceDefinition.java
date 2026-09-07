package dev.zhangping.biomedicalradar.collector;

import java.util.Map;

import static dev.zhangping.biomedicalradar.domain.DomainEnums.*;

public record SourceDefinition(
        String id,
        String name,
        boolean enabled,
        SourceTier tier,
        SourceType type,
        Region region,
        String language,
        String connector,
        String url,
        String scheduleGroup,
        LegalBasis legalBasis,
        int timeoutSeconds,
        String fixture,
        Map<String, String> options
) {
    public SourceDefinition {
        options = Map.copyOf(options == null ? Map.of() : options);
    }
}
