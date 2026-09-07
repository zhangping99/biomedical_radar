package dev.zhangping.biomedicalradar.collector;

public interface TranslationProvider {
    GeneratedText translateTitle(String original, String language);
}
