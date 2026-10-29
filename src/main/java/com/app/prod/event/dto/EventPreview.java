package com.app.prod.event.dto;

/**
 * What a chat app shows for a shared event link - the meta tags and the image are both drawn from it,
 * so they never disagree. Holds only what anyone holding the link may see anyway.
 *
 * @param found       false for a slug that leads nowhere; the preview is then the app's own
 * @param when        null when the event has no date to show
 * @param where       null when the event has no place to show
 * @param description the og:description - the facts above in one line, plus the start of the event's own text
 */
public record EventPreview(
        boolean found,
        String siteName,
        String title,
        String when,
        String where,
        String description,
        String pageUrl,
        String imageUrl,
        String locale
) {
}
