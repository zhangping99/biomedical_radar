package dev.zhangping.biomedicalradar.collector;

import java.util.Locale;

public final class NoKeyProviders implements TranslationProvider, SummaryProvider {
    @Override
    public GeneratedText translateTitle(String original, String language) {
        return isChinese(language) ? GeneratedText.unchanged(original) : GeneratedText.unavailable();
    }

    @Override
    public GeneratedText summarize(String sourceDescription, String language) {
        if (!isChinese(language) || sourceDescription == null || sourceDescription.isBlank()) {
            return GeneratedText.unavailable();
        }
        String clean = sourceDescription.replaceAll("\\s+", " ").trim();
        return GeneratedText.unchanged(clean.length() <= 180 ? clean : clean.substring(0, 177) + "…");
    }

    private boolean isChinese(String language) {
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("zh");
    }
}
