package com.app.prod.event.service;

import com.app.prod.event.config.EventShareProperties;
import com.app.prod.event.dto.EventPreview;
import com.app.prod.event.dto.EventRow;
import com.app.prod.event.enums.EventStatus;
import com.app.prod.event.enums.SchedulingMode;
import com.app.prod.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Decides what a shared event link reveals to a chat app - and so to everyone in that chat, link
 * clicked or not. Only what the event page shows to a guest holding the link goes in: never the
 * online meeting url, never who is coming.
 * <p>
 * Nothing that changes while the link is passed around goes in either, how many people are going
 * above all: a chat app keeps the preview from the moment the link was sent, so a count would stay
 * "1 osoba" in that chat long after the group filled up.
 */
@Service
@RequiredArgsConstructor
public class EventPreviewService {

    /** Bump when the image changes its look, so chat apps fetch it again instead of keeping the old one. */
    private static final String IMAGE_REVISION = "5";
    private static final int DESCRIPTION_LIMIT = 200;
    private static final String SEPARATOR = " · ";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final EventRepository eventRepository;
    private final EventShareProperties properties;
    private final MessageSource messageSource;
    private final Clock clock;

    public EventPreview preview(String slug) {
        return eventRepository.findRowBySlug(slug)
                .map(this::of)
                .orElseGet(() -> missing(slug));
    }

    private EventPreview of(EventRow row) {
        String facts = Stream.of(when(row), where(row))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(SEPARATOR));
        String description = row.description() == null || row.description().isBlank()
                ? facts
                : facts + (facts.isEmpty() ? "" : " — ") + row.description().strip();

        return new EventPreview(
                true,
                properties.getSiteName(),
                row.name(),
                shorten(description),
                pageUrl(row.slug()),
                imageUrl(row.slug(), version(row.name())),
                properties.getLocale().toString()
        );
    }

    /** Same page as for a real event, so a link that stopped working still unfurls as the app. */
    private EventPreview missing(String slug) {
        return new EventPreview(
                false,
                properties.getSiteName(),
                properties.getSiteName(),
                message("share.event.missing"),
                pageUrl(slug),
                imageUrl(slug, version()),
                properties.getLocale().toString()
        );
    }

    private String when(EventRow row) {
        boolean choosingDate = row.schedulingMode() == SchedulingMode.POLL && row.status() != EventStatus.CONFIRMED;
        if (choosingDate) {
            return row.votingDeadline() == null
                    ? message("share.event.voting")
                    : message("share.event.voting-until", deadline(row.votingDeadline()));
        }
        if (row.startDate() == null) {
            return null;
        }

        LocalDateTime start = row.startDate();
        LocalDateTime end = row.endDate();
        String startText = day(start) + SEPARATOR + TIME.format(start);

        if (end == null) {
            return startText;
        }
        return start.toLocalDate().equals(end.toLocalDate())
                ? startText + "–" + TIME.format(end)
                : startText + " – " + day(end) + SEPARATOR + TIME.format(end);
    }

    private String where(EventRow row) {
        if (row.locationName() != null && !row.locationName().isBlank()) {
            return row.locationName().strip();
        }
        return row.onlineUrl() != null && !row.onlineUrl().isBlank()
                ? message("share.event.online")
                : null;
    }

    /** The year only when it is not this one - "sobota, 15 listopada" reads better than a full date. */
    private String day(LocalDateTime dateTime) {
        boolean thisYear = dateTime.getYear() == LocalDate.now(clock).getYear();
        return DateTimeFormatter.ofPattern(thisYear ? "EEEE, d MMMM" : "EEEE, d MMMM yyyy", properties.getLocale())
                .format(dateTime);
    }

    /** No weekday - it would have to be declined after "do" ("do czwartku"), a month already is. */
    private String deadline(LocalDateTime dateTime) {
        boolean thisYear = dateTime.getYear() == LocalDate.now(clock).getYear();
        return DateTimeFormatter.ofPattern(thisYear ? "d MMMM" : "d MMMM yyyy", properties.getLocale()).format(dateTime)
                + ", " + TIME.format(dateTime);
    }

    private static String shorten(String text) {
        String oneLine = text.replaceAll("\\s+", " ");
        return oneLine.length() <= DESCRIPTION_LIMIT
                ? oneLine
                : oneLine.substring(0, DESCRIPTION_LIMIT - 1).stripTrailing() + "…";
    }

    /** Everything the image shows; a change of any of it is a new image url, which chat apps fetch anew. */
    private static String version(String... shown) {
        String joined = IMAGE_REVISION + "|" + String.join("|", Stream.of(shown).map(String::valueOf).toList());
        return DigestUtils.md5DigestAsHex(joined.getBytes(StandardCharsets.UTF_8)).substring(0, 12);
    }

    private String pageUrl(String slug) {
        String base = properties.getLinkBaseUrl();
        return (base.endsWith("/") ? base : base + "/") + slug;
    }

    private String imageUrl(String slug, String version) {
        return pageUrl(slug) + "/preview.png?v=" + version;
    }

    private String message(String key, Object... arguments) {
        Locale locale = properties.getLocale();
        return messageSource.getMessage(key, arguments, locale);
    }
}
