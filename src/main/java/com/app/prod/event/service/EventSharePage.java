package com.app.prod.event.service;

import com.app.prod.event.config.EventShareProperties;
import com.app.prod.event.dto.EventPreview;
import com.app.prod.event.web.FrontendIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The page behind {@code /e/{slug}}: the frontend's own index.html with the event's meta tags put in.
 * <p>
 * Chat apps read the meta tags and never run the scripts; a browser runs the scripts and the app
 * takes over as on any other route. Everyone gets the same page - there is no telling bots apart by
 * their user agent, which is a guess at best.
 */
@Service
@RequiredArgsConstructor
public class EventSharePage {

    private static final String TEMPLATE = "share/event-meta";
    private static final Pattern HEAD = Pattern.compile("<head(\\s[^>]*)?>", Pattern.CASE_INSENSITIVE);

    private final EventPreviewService eventPreviewService;
    private final FrontendIndex frontendIndex;
    private final SpringTemplateEngine templateEngine;
    private final EventShareProperties properties;

    /** @param complete false when the frontend could not be reached and the page cannot start the app */
    public record Page(String html, boolean complete) {
    }

    public Page render(String slug) {
        EventPreview preview = eventPreviewService.preview(slug);
        String meta = templateEngine.process(TEMPLATE, new Context(properties.getLocale(), Map.of("preview", preview)));

        Optional<String> index = frontendIndex.html();
        return index
                .map(html -> new Page(inject(html, meta), true))
                .orElseGet(() -> new Page(fallback(meta), false));
    }

    /**
     * Right after {@code <head>}, ahead of whatever the frontend declares itself: a chat app takes the
     * first og:title it finds, and a browser the first title.
     */
    static String inject(String html, String meta) {
        Matcher head = HEAD.matcher(html);
        if (!head.find()) {
            return "<head>\n" + meta + "</head>\n" + html;
        }
        return html.substring(0, head.end()) + "\n" + meta + html.substring(head.end());
    }

    /** Enough for a chat app to unfurl the link; a person is asked to try again in a moment. */
    private String fallback(String meta) {
        String language = properties.getLocale().getLanguage();
        String hint = "pl".equals(language)
                ? "Nie udało się załadować wydarzenia. Odśwież stronę za chwilę."
                : "The event could not be loaded. Please refresh the page in a moment.";

        return "<!doctype html>\n<html lang=\"" + language + "\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + meta + "</head>\n<body>\n<p>" + hint + "</p>\n</body>\n</html>\n";
    }
}
