package dev.zhangping.biomedicalradar.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static dev.zhangping.biomedicalradar.domain.DomainEnums.*;
import static org.assertj.core.api.Assertions.assertThat;

class ImportanceScorerTest {
    @Test
    void excludesTierCFromMustReadThreshold() {
        Instant now = Instant.parse("2026-09-07T00:00:00Z");
        Article article = new Article("id", "tip", "Tip", SourceTier.C, SourceType.media,
                "https://example.com/a", "https://example.com/a", "Safety alert", null, null, null,
                "en", Region.GLOBAL, Category.quality_safety, List.of("safety_alert"), List.of(), List.of(),
                now, now, "hash", false, null, null, VerificationStatus.unreviewed,
                OriginalAccessStatus.unknown, LegalBasis.manual_link, List.of(), 0, List.of());

        ImportanceScorer.Score score = new ImportanceScorer().score(article, now);

        assertThat(score.value()).isLessThan(40);
        assertThat(score.reasons()).contains("C级线索不进入今日必读");
    }
}
