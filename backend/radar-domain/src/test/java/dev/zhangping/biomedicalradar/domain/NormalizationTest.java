package dev.zhangping.biomedicalradar.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizationTest {
    @Test
    void normalizesTrackingParametersAndWhitespace() {
        assertThat(UrlNormalizer.normalize("http://Example.com/news/?utm_source=x&b=2&a=1#part"))
                .isEqualTo("https://example.com/news?a=1&b=2");
        assertThat(TextNormalizer.normalize("  Phase\u3000  III   result  ")).isEqualTo("Phase III result");
    }
}
