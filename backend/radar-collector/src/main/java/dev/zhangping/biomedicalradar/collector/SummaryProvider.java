package dev.zhangping.biomedicalradar.collector;

public interface SummaryProvider {
    GeneratedText summarize(String sourceDescription, String language);
}
