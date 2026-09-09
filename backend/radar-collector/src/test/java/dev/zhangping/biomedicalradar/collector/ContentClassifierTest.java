package dev.zhangping.biomedicalradar.collector;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static dev.zhangping.biomedicalradar.domain.DomainEnums.*;
import static org.assertj.core.api.Assertions.assertThat;

class ContentClassifierTest {
    @Test
    void filtersAdministrativeNoticesButRetainsResearchAndTrialRecruitment() {
        SourceDefinition source = new SourceDefinition("hospital", "Hospital", true, SourceTier.B,
                SourceType.media, Region.CN, "zh-CN", "html", "https://example.test/", "hospital",
                LegalBasis.public_notice, 20, "example.html", Map.of("entityType", "hospital"));
        ContentClassifier classifier = new ContentClassifier();
        assertThat(classifier.relevantTitle(source, "2026年招聘公告")).isFalse();
        assertThat(classifier.relevantTitle(source, "采购公告：实验室设备")).isFalse();
        assertThat(classifier.relevantTitle(source, "ASCO大会公布临床研究结果")).isTrue();
        assertThat(classifier.relevantTitle(source, "临床试验受试者招募")).isTrue();
        assertThat(classifier.relevantTitle(source, "Company to Present at Healthcare Conference")).isFalse();
        assertThat(classifier.relevantTitle(source, "Company presents Phase 3 results at ASCO")).isTrue();
    }
}
