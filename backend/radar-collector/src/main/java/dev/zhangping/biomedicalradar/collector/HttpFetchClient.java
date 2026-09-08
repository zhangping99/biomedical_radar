package dev.zhangping.biomedicalradar.collector;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public final class HttpFetchClient implements FetchClient {
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_RESPONSE_CHARACTERS = 5_000_000;
    private static final String USER_AGENT =
            "BiomedicalRadar/1.0 (+https://github.com/zhangping99/biomedical_radar; zhangping99@users.noreply.github.com)";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final Map<String, Semaphore> hostPermits = new ConcurrentHashMap<>();

    @Override
    public String fetch(SourceDefinition source) throws IOException, InterruptedException {
        return fetch(source, URI.create(source.url()));
    }

    @Override
    public String fetch(SourceDefinition source, URI uri) throws IOException, InterruptedException {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IOException("UNSAFE_SOURCE_URL");
        }
        Semaphore hostPermit = hostPermits.computeIfAbsent(uri.getHost().toLowerCase(), ignored -> new Semaphore(1));
        hostPermit.acquire();
        try {
            return fetchWithRetries(source, uri);
        } finally {
            hostPermit.release();
        }
    }

    private String fetchWithRetries(SourceDefinition source, URI uri) throws IOException, InterruptedException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(Math.max(5, source.timeoutSeconds())))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json, application/rss+xml, application/xml, text/html;q=0.9, */*;q=0.5")
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    if (response.body().length() > MAX_RESPONSE_CHARACTERS) {
                        throw new IOException("RESPONSE_TOO_LARGE");
                    }
                    return response.body();
                }
                lastFailure = new IOException("HTTP_" + response.statusCode());
                if (response.statusCode() < 500 && response.statusCode() != 429) {
                    break;
                }
            } catch (IOException exception) {
                lastFailure = exception;
            }
            if (attempt < MAX_ATTEMPTS) {
                Thread.sleep(250L * attempt);
            }
        }
        throw lastFailure == null ? new IOException("FETCH_FAILED") : lastFailure;
    }
}
