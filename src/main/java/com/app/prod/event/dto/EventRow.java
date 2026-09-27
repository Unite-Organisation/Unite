package com.app.prod.event.dto;

import com.app.prod.event.enums.EventStatus;
import com.app.prod.event.enums.SchedulingMode;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventRow(
        UUID id,
        String slug,
        String name,
        String description,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String locationName,
        String onlineUrl,
        Integer maxAttendees,
        boolean waitlistEnabled,
        SchedulingMode schedulingMode,
        EventStatus status,
        Integer minAttendees,
        LocalDateTime votingDeadline,
        UUID selectedSlotId,
        int goingCount,
        LocalDateTime createdAt
) {
}
