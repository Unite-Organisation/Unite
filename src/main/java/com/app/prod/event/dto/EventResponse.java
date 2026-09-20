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
        int goingCount,
        LocalDateTime createdAt,
        MemberResponse me
) {

    public EventResponse withMe(MemberResponse me) {
        return new EventResponse(slug, name, description, startDate, endDate, locationName, onlineUrl,
                maxAttendees, waitlistEnabled, goingCount, createdAt, me);
    }
}
