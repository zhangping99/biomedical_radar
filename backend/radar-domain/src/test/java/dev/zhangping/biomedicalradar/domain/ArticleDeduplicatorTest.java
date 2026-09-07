package dev.zhangping.biomedicalradar.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static dev.zhangping.biomedicalradar.domain.DomainEnums.*;
import static org.assertj.core.api.Assertions.assertThat;

class ArticleDeduplicatorTest {
    @Test
    void mergesOnlyCertainCrossSourceMatchesAndPreservesLinks() {
        Article first = article("a", "fda", "https://fda.gov/a", "Drug X receives approval", "approval",
                Instant.parse("2026-09-01T10:00:00Z"));
        Article duplicate = article("b", "ema", "https://ema.eu/a", "Drug X receives approval", "approval",
                Instant.parse("2026-09-02T10:00:00Z"));
        Article uncertain = article("c", "ema", "https://ema.eu/b", "Drug X receives approval", "meeting",
                Instant.parse("2026-09-02T10:00:00Z"));

        ArticleDeduplicator.Result result = new ArticleDeduplicator().deduplicate(List.of(first, duplicate, uncertain));

        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(result.articles()).hasSize(2);
        assertThat(result.articles().get(0).sourceLinks()).hasSize(2);
    }

    private Article article(String id, String sourceId, String url, String title, String event, Instant time) {
        return new Article(id, sourceId, sourceId.toUpperCase(), SourceTier.A, SourceType.regulator,
                url, url, title, null, null, null, "en", Region.GLOBAL,
                Category.drug_rd, List.of(event), List.of(), List.of(), time, time,
                ArticleIdentity.contentHash(title), false, null, null, VerificationStatus.auto_checked,
                OriginalAccessStatus.reachable, LegalBasis.official_api,
                List.of(new SourceLink(sourceId, sourceId.toUpperCase(), SourceTier.A, url)), 0, List.of());
    }
}
