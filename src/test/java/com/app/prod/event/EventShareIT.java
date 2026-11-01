package com.app.prod.event;

import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.event.EventApi.Event;
import com.app.prod.event.dto.SlotRequest;
import com.app.prod.event.service.EventPreviewImage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.EVENT;

/**
 * A shared event link as a chat app fetches it: over real http and at {@code /event/{slug}}, outside the
 * servlet context path - which is the whole point, since that is the path the frontend hosting
 * forwards. A stand-in frontend serves the index.html the page is built from.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EventShareIT extends IntegrationTest {

    private static final String INDEX = """
            <!doctype html>
            <html lang="pl">
            <head>
              <meta charset="utf-8">
              <title>Unitly app</title>
            </head>
            <body><app-root></app-root><script src="main-%s.js"></script></body>
            </html>
            """;

    private static final HttpServer frontend = startFrontend();
    private static volatile String frontendBuild = "aaaa";
    private static volatile boolean frontendUp = true;
    private static final AtomicInteger frontendHits = new AtomicInteger();

    @LocalServerPort
    private int port;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DSLContext dslContext;
    @Autowired
    private MutableClock clock;

    private final HttpClient http = HttpClient.newHttpClient();
    private EventApi api;

    @DynamicPropertySource
    static void frontendProperties(DynamicPropertyRegistry registry) {
        registry.add("event.share.frontend-index-url",
                () -> "http://localhost:" + frontend.getAddress().getPort() + "/index.html");
        registry.add("event.share.link-base-url", () -> "https://unitly.test/event");
    }

    @AfterAll
    static void stopFrontend() {
        frontend.stop(0);
    }

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(EVENT).execute();
        api = new EventApi(mvc, objectMapper);
        frontendBuild = "aaaa";
        frontendUp = true;
        // whatever an earlier test left in the cache is past its interval now - forward only, a clock
        // set back would make that copy fresh again
        clock.advance(Duration.ofMinutes(5));
    }

    @Test
    void sharedLinkIsTheFrontendWithTheEventsMetaTagsInFront() throws Exception {
        Event event = datedEvent("Planszówki u Kasi", "Klub Pod Wiatrakiem", "Gramy w Wsiąść do pociągu.");

        HttpResponse<String> page = get("/event/" + event.slug());

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("text/html").containsIgnoringCase("charset=UTF-8"));
        assertThat(page.headers().firstValue("Cache-Control")).hasValueSatisfying(cache ->
                assertThat(cache).contains("s-maxage=60").contains("public"));

        String html = page.body();
        assertThat(html)
                .contains("<meta property=\"og:title\" content=\"Planszówki u Kasi\">")
                .contains("<meta property=\"og:url\" content=\"https://unitly.test/event/" + event.slug() + "\">")
                .contains("<meta property=\"og:image\" content=\"https://unitly.test/event/" + event.slug() + "/preview.png?v=")
                .contains("<meta name=\"twitter:card\" content=\"summary_large_image\">")
                .contains("<meta name=\"robots\" content=\"noindex\">")
                .as("the app still starts from it")
                .contains("<script src=\"main-aaaa.js\"></script>");
        assertThat(description(html))
                .contains("Klub Pod Wiatrakiem")
                .contains("Gramy w Wsiąść do pociągu.");
        assertThat(html.indexOf("og:title"))
                .as("ahead of the frontend's own title, so it is the one taken")
                .isLessThan(html.indexOf("<title>Unitly app</title>"));
    }

    @Test
    void whatTheHostTypedCannotBreakOutOfTheMetaTags() throws Exception {
        Event event = datedEvent("\"><script>alert(1)</script>", null, null);

        String html = get("/event/" + event.slug()).body();

        assertThat(html)
                .doesNotContain("<script>alert(1)</script>")
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void onlineUrlNeverReachesThePreview() throws Exception {
        Event event = api.createDatedEvent("Kuba", LocalDateTime.now(clock).plusDays(3), null, false);
        dslContext.update(EVENT)
                .set(EVENT.ONLINE_URL, "https://meet.example/secret-room")
                .where(EVENT.PUBLIC_SLUG.eq(event.slug()))
                .execute();

        String html = get("/event/" + event.slug()).body();

        assertThat(html).doesNotContain("secret-room");
        assertThat(description(html)).contains("Online");
    }

    @Test
    void eventStillChoosingItsDateSaysSo() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        Event event = api.createEventChoosingItsDate("Kuba", 4, now.plusDays(2), List.of(
                new SlotRequest(now.plusDays(7), null),
                new SlotRequest(now.plusDays(8), null)));

        assertThat(description(get("/event/" + event.slug()).body())).startsWith("Termin w głosowaniu do ");
    }

    @Test
    void previewImageIsServedUnderTheSharedLinkToo() throws Exception {
        Event event = datedEvent("Planszówki u Kasi", "Klub Pod Wiatrakiem", null);

        HttpResponse<byte[]> response = http.send(request("/event/" + event.slug() + "/preview.png?v=abc"),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("image/png");
        assertThat(response.headers().firstValue("Cache-Control")).hasValueSatisfying(cache ->
                assertThat(cache).contains("max-age=86400").contains("public"));
        assertThat(response.body().length)
                .as("WhatsApp drops images much past 300 kB")
                .isLessThan(300 * 1024);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
        assertThat(image.getWidth()).isEqualTo(EventPreviewImage.WIDTH);
        assertThat(image.getHeight()).isEqualTo(EventPreviewImage.HEIGHT);
    }

    @Test
    void imageUrlChangesWithWhatTheImageShows() throws Exception {
        Event event = datedEvent("Planszówki u Kasi", null, null);
        String before = imageUrl(get("/event/" + event.slug()).body());

        dslContext.update(EVENT).set(EVENT.NAME, "Planszówki u Kasi – przeniesione")
                .where(EVENT.PUBLIC_SLUG.eq(event.slug())).execute();
        String after = imageUrl(get("/event/" + event.slug()).body());

        assertThat(after).isNotEqualTo(before);
    }

    @Test
    void oldShortPathKeepsWorkingUntilTheHostingRedirectsIt() throws Exception {
        Event event = datedEvent("Planszówki u Kasi", null, null);

        assertThat(get("/e/" + event.slug()).body())
                .contains("<meta property=\"og:url\" content=\"https://unitly.test/event/" + event.slug() + "\">");
    }

    @Test
    void deeperFrontendRoutesGetTheSamePage() throws Exception {
        Event event = datedEvent("Planszówki u Kasi", null, null);

        assertThat(get("/event/" + event.slug() + "/members").body())
                .contains("<meta property=\"og:title\" content=\"Planszówki u Kasi\">");
    }

    @Test
    void linkThatLeadsNowhereStillUnfurlsAsTheApp() throws Exception {
        HttpResponse<String> page = get("/event/doesNotExist123");

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("<meta property=\"og:title\" content=\"Unitly\">");
        assertThat(http.send(request("/event/doesNotExist123/preview.png"), HttpResponse.BodyHandlers.discarding()).statusCode())
                .isEqualTo(200);
    }

    @Test
    void frontendDeployIsPickedUpAfterTheRefreshInterval() throws Exception {
        Event event = datedEvent("Planszówki u Kasi", null, null);
        assertThat(get("/event/" + event.slug()).body()).contains("main-aaaa.js");

        frontendBuild = "bbbb";
        assertThat(get("/event/" + event.slug()).body())
                .as("within the interval the copy in memory is trusted")
                .contains("main-aaaa.js");

        clock.advance(Duration.ofMinutes(1));
        assertThat(get("/event/" + event.slug()).body()).contains("main-bbbb.js");
    }

    @Test
    void unchangedFrontendIsOnlyRevalidated() throws Exception {
        Event event = datedEvent("Planszówki u Kasi", null, null);
        get("/event/" + event.slug());
        int before = frontendHits.get();

        clock.advance(Duration.ofMinutes(1));
        assertThat(get("/event/" + event.slug()).body()).contains("main-aaaa.js");

        assertThat(frontendHits.get() - before).isEqualTo(1);
    }

    @Test
    void frontendGoingDownKeepsTheLastCopy() throws Exception {
        Event event = datedEvent("Planszówki u Kasi", null, null);
        get("/event/" + event.slug());

        frontendUp = false;
        clock.advance(Duration.ofMinutes(1));
        HttpResponse<String> page = get("/event/" + event.slug());

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("main-aaaa.js").contains("og:title");
    }

    @Test
    void restOfTheApiIsWhereItWas() throws Exception {
        Event event = datedEvent("Planszówki u Kasi", null, null);

        HttpResponse<String> response = get("/v1/api/public/event/" + event.slug());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"name\":\"Planszówki u Kasi\"");
    }

    // ---------------------------------------------------------------- helpers

    private Event datedEvent(String name, String location, String description) {
        Event event = api.createDatedEvent("Kuba", LocalDateTime.now(clock).plusDays(3), null, false);
        dslContext.update(EVENT)
                .set(EVENT.NAME, name)
                .set(EVENT.LOCATION_NAME, location)
                .set(EVENT.DESCRIPTION, description)
                .where(EVENT.PUBLIC_SLUG.eq(event.slug()))
                .execute();
        return event;
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(request(path), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpRequest request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
    }

    private static String description(String html) {
        return attribute(html, "<meta property=\"og:description\" content=\"");
    }

    private static String imageUrl(String html) {
        return attribute(html, "<meta property=\"og:image\" content=\"");
    }

    private static String attribute(String html, String prefix) {
        int start = html.indexOf(prefix);
        assertThat(start).as("%s is in the page", prefix).isNotNegative();
        start += prefix.length();
        return html.substring(start, html.indexOf('"', start));
    }

    /** Serves index.html with an ETag per build and answers a matching If-None-Match with a 304. */
    private static HttpServer startFrontend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/index.html", exchange -> {
                frontendHits.incrementAndGet();
                if (!frontendUp) {
                    exchange.sendResponseHeaders(503, -1);
                    exchange.close();
                    return;
                }
                String etag = "\"" + frontendBuild + "\"";
                exchange.getResponseHeaders().add("ETag", etag);
                if (etag.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                    exchange.sendResponseHeaders(304, -1);
                    exchange.close();
                    return;
                }
                byte[] body = INDEX.formatted(frontendBuild).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
