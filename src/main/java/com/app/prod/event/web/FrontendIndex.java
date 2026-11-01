package com.app.prod.event.web;

import com.app.prod.event.config.EventShareProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The frontend's index.html, which a shared event page is built from.
 * <p>
 * It is fetched from the frontend hosting and kept in memory, but only briefly: the bundles it points
 * at carry content hashes and the old ones are gone after every frontend deploy, so a stale copy
 * would serve a page whose scripts no longer exist. Past {@code indexRefreshInterval} it is
 * revalidated with its ETag, which costs a 304 when nothing changed.
 * <p>
 * Fetched inside the request that needs it, never in the background - Cloud Run throttles the CPU
 * outside of requests. When the frontend cannot be reached the last known copy is used, and only
 * when there never was one the caller gets nothing.
 */
@Slf4j
@Component
public class FrontendIndex {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient;
    private final EventShareProperties properties;
    private final Clock clock;
    private final ReentrantLock refreshing = new ReentrantLock();

    private volatile Snapshot snapshot;

    public FrontendIndex(EventShareProperties properties, Clock clock) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        // over plain http the client asks for an h2c upgrade, which a node dev
                        // server (ng serve) never answers - the request just times out
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build());
        requestFactory.setReadTimeout(TIMEOUT);

        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.properties = properties;
        this.clock = clock;
    }

    public Optional<String> html() {
        Snapshot current = snapshot;
        if (current != null && current.freshAt(clock.instant(), properties.getIndexRefreshInterval())) {
            return Optional.of(current.html());
        }

        // one request revalidates, the others meanwhile go on with what there is
        if (current != null && !refreshing.tryLock()) {
            return Optional.of(current.html());
        }
        if (current == null) {
            refreshing.lock();
        }
        try {
            snapshot = fetch(snapshot);
        } finally {
            refreshing.unlock();
        }
        return Optional.ofNullable(snapshot).map(Snapshot::html);
    }

    private Snapshot fetch(Snapshot known) {
        Instant now = clock.instant();
        if (known != null && known.freshAt(now, properties.getIndexRefreshInterval())) {
            return known;
        }

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(properties.getFrontendIndexUrl())
                    .headers(headers -> {
                        if (known != null && known.etag() != null) {
                            headers.setIfNoneMatch(known.etag());
                        }
                    })
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode() == HttpStatus.NOT_MODIFIED && known != null) {
                return kept(known, now);
            }
            if (response.getBody() == null || response.getBody().isBlank()) {
                log.error("Frontend index at {} came back empty ({})", properties.getFrontendIndexUrl(), response.getStatusCode());
                return kept(known, now);
            }
            return new Snapshot(response.getBody(), response.getHeaders().getFirst(HttpHeaders.ETAG), now);
        } catch (Exception e) {
            log.error("Frontend index at {} could not be fetched, {}: {}", properties.getFrontendIndexUrl(),
                    known == null ? "no copy to fall back on" : "keeping the last copy", e.getMessage());
            return kept(known, now);
        }
    }

    /** A frontend that is down is asked again after the interval, not by every request waiting out the timeout. */
    private static Snapshot kept(Snapshot known, Instant now) {
        return known == null ? null : new Snapshot(known.html(), known.etag(), now);
    }

    private record Snapshot(String html, String etag, Instant fetchedAt) {
        boolean freshAt(Instant now, Duration interval) {
            return fetchedAt.plus(interval).isAfter(now);
        }
    }
}
