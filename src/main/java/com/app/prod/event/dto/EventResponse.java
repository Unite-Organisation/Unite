package com.app.prod.event.dto;

import com.app.prod.event.enums.EventStatus;
import com.app.prod.event.enums.SchedulingMode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        boolean requiresApproval,
        SchedulingMode schedulingMode,
        EventStatus status,
        Integer minAttendees,
        LocalDateTime votingDeadline,
        UUID selectedSlotId,
        List<EventSlotResponse> slots,
        int goingCount,
        LocalDateTime createdAt,
        MemberResponse me,
        List<SlotVote> myVotes
) {
}
