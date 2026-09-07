package dev.zhangping.biomedicalradar.collector;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoKeyProvidersTest {
    @Test
    void doesNotPretendToTranslateForeignTextWithoutAProvider() {
        NoKeyProviders providers = new NoKeyProviders();

        assertThat(providers.translateTitle("New medicine", "en").text()).isNull();
        assertThat(providers.translateTitle("新药获批", "zh-CN").text()).isEqualTo("新药获批");
        assertThat(providers.translateTitle("New medicine", "en").generated()).isFalse();
    }
}
