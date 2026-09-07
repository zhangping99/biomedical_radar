package dev.zhangping.biomedicalradar.collector;

import java.time.Instant;

public record GeneratedText(String text, boolean generated, String generatorVersion, Instant generatedAt) {
    public static GeneratedText unchanged(String text) {
        return new GeneratedText(text, false, null, null);
    }

    public static GeneratedText unavailable() {
        return unchanged(null);
    }
}
