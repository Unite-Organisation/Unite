package com.app.prod.event.api;

import com.app.prod.event.dto.EventPreview;
import com.app.prod.event.service.EventPreviewImage;
import com.app.prod.event.service.EventPreviewService;
import com.app.prod.event.service.EventSharePage;
import com.app.prod.utils.CookieSession;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * What a chat app sees of a shared event link. The frontend hosting forwards {@code /e/{slug}} here
 * (see {@code EventShareRewrite}), so the link people copy from the address bar unfurls too.
 */
@RestController
@RequestMapping("/public/share/event")
@RequiredArgsConstructor
@Tag(name = "Public events")
public class EventShareController {

    private static final MediaType HTML = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private final EventSharePage eventSharePage;
    private final EventPreviewService eventPreviewService;
    private final EventPreviewImage eventPreviewImage;

    @GetMapping(value = "/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    @CookieSession(
            value = CookieSession.Mode.NONE,
            note =  "The frontend's index.html with the event's meta tags - what /e/{slug} serves to people and chat apps alike"
    )
    public ResponseEntity<String> getPage(@PathVariable String slug) {
        EventSharePage.Page page = eventSharePage.render(slug);

        // briefly at the CDN, never in the browser: the page points at the frontend's current bundles,
        // and one that could not reach the frontend must not stick around at all
        CacheControl cacheControl = page.complete()
                ? CacheControl.maxAge(Duration.ZERO).sMaxAge(Duration.ofMinutes(1)).cachePublic()
                : CacheControl.noStore();

        return ResponseEntity.ok()
                .contentType(HTML)
                .cacheControl(cacheControl)
                .body(page.html());
    }

    @GetMapping(value = "/{slug}/preview.png", produces = MediaType.IMAGE_PNG_VALUE)
    @CookieSession(
            value = CookieSession.Mode.NONE,
            note =  "The og:image. Its url carries a version of what it shows, so it can be cached for long"
    )
    public ResponseEntity<byte[]> getPreviewImage(@PathVariable String slug) {
        EventPreview preview = eventPreviewService.preview(slug);

        CacheControl cacheControl = preview.found()
                ? CacheControl.maxAge(Duration.ofDays(1)).cachePublic()
                : CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(cacheControl)
                .body(eventPreviewImage.render(preview));
    }
}
