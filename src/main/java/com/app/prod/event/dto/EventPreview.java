package com.app.prod.event.dto;


public record EventPreview(
        boolean found,
        String siteName,
        String title,
        String description,
        String pageUrl,
        String imageUrl,
        String locale
) {
}
