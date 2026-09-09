package dev.zhangping.biomedicalradar.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizationTest {
    @Test
    void serializesChinesePathsAsValidAsciiUris() {
        assertThat(UrlNormalizer.normalize("https://example.com/研究/"))
                .isEqualTo("https://example.com/%E7%A0%94%E7%A9%B6");
        assertThat(UrlNormalizer.normalize("https://example.com/%e7%a0%94%e7%a9%b6/"))
                .isEqualTo("https://example.com/%E7%A0%94%E7%A9%B6");
    }

    @Test
    void normalizesTrackingParametersAndWhitespace() {
        assertThat(UrlNormalizer.normalize("http://Example.com/news/?utm_source=x&b=2&a=1#part"))
                .isEqualTo("https://example.com/news?a=1&b=2");
        assertThat(TextNormalizer.normalize("  Phase\u3000  III   result  ")).isEqualTo("Phase III result");
    }
}
