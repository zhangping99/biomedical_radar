package dev.zhangping.biomedicalradar.collector;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpFetchClient implements FetchClient {
    private static final int MAX_ATTEMPTS = 3;
    private static final String USER_AGENT =
            "BiomedicalRadar/1.0 (+https://github.com/zhangping99/biomedical_radar; zhangping99@users.noreply.github.com)";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @Override
    public String fetch(SourceDefinition source) throws IOException, InterruptedException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(source.url()))
                    .timeout(Duration.ofSeconds(Math.max(5, source.timeoutSeconds())))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json, application/rss+xml, application/xml, text/html;q=0.9, */*;q=0.5")
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
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
