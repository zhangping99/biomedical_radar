package dev.zhangping.biomedicalradar.collector;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static dev.zhangping.biomedicalradar.domain.DomainEnums.*;
import static org.assertj.core.api.Assertions.assertThat;

class CollectorConcurrencyTest {
    @Test
    void collectionUsesTheConfiguredBoundedConcurrency() {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        FetchClient fetchClient = source -> {
            int current = active.incrementAndGet();
            maximum.accumulateAndGet(current, Math::max);
            try {
                Thread.sleep(40);
                return """
                        <rss version="2.0"><channel><title>fixture</title><item>
                          <guid>%s</guid><title>Research update from %s</title>
                          <link>https://example.test/articles/%s</link>
                        </item></channel></rss>
                        """.formatted(source.id(), source.id(), source.id());
            } finally {
                active.decrementAndGet();
            }
        };
        List<SourceDefinition> sources = IntStream.range(0, 6)
                .mapToObj(index -> sourceDefinition("source-" + index))
                .toList();
        NoKeyProviders providers = new NoKeyProviders();
        CollectorService service = new CollectorService(new ConnectorRegistry(), providers, providers,
                new ContentClassifier(), Clock.systemUTC());

        CollectionResult result = service.collect(sources, fetchClient, 2);

        assertThat(maximum).hasValue(2);
        assertThat(result.sourceHealth()).hasSize(6).allMatch(health -> "ok".equals(health.status()));
    }

    private SourceDefinition sourceDefinition(String id) {
        return new SourceDefinition(id, id, true, SourceTier.A, SourceType.journal, Region.GLOBAL,
                "en", "rss", "https://example.test/" + id, "research", LegalBasis.rss_allowed,
                10, "unused.xml", Map.of());
    }
}
