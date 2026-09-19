package com.app.prod.event.dto;

import java.time.LocalDateTime;

public record EventResponse(
        String slug,
        String name,
        String description,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String locationName,
        String onlineUrl,
        Integer maxAttendees,
        boolean waitlistEnabled,
        LocalDateTime createdAt
) {
}
