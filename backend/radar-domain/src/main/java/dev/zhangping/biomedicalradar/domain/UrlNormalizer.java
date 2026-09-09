package dev.zhangping.biomedicalradar.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class UrlNormalizer {
    private static final Set<String> TRACKING_KEYS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "gclid", "fbclid", "mc_cid", "mc_eid"
    );

    private UrlNormalizer() {
    }

    public static String normalize(String rawUrl) {
        try {
            URI input = URI.create(rawUrl.trim());
            String scheme = input.getScheme() == null ? "https" : input.getScheme().toLowerCase();
            if ("http".equals(scheme)) {
                scheme = "https";
            }
            String host = input.getHost() == null ? "" : input.getHost().toLowerCase();
            String path = input.getPath() == null || input.getPath().isBlank() ? "/" : input.getPath();
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            String query = cleanQuery(input.getRawQuery());
            return new URI(scheme, input.getUserInfo(), host, input.getPort(), path, query, null).toASCIIString();
        } catch (IllegalArgumentException | URISyntaxException exception) {
            return rawUrl == null ? "" : rawUrl.trim();
        }
    }

    private static String cleanQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        String cleaned = Arrays.stream(rawQuery.split("&"))
                .filter(part -> !part.isBlank())
                .filter(part -> !TRACKING_KEYS.contains(part.split("=", 2)[0].toLowerCase()))
                .sorted()
                .collect(Collectors.joining("&"));
        return cleaned.isBlank() ? null : cleaned;
    }
}
