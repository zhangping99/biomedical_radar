package dev.zhangping.biomedicalradar.domain;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {
    private TextNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String fingerprint(String value) {
        return normalize(value).toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{S}\\s]", "");
    }
}
