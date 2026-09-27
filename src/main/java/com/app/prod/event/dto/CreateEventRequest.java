package com.app.prod.event.dto;

import com.app.prod.event.enums.SchedulingMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateEventRequest(
        @NotBlank @Size(max = 256) String name,
        String description,
        LocalDateTime startDate,
        LocalDateTime endDate,
        @Size(max = 256) String location,
        @Size(max = 2048) String onlineUrl,
        @Positive Integer maxAttendees,
        boolean waitlistEnabled,
        @Size(max = 60) String displayName,
        @Valid DeviceFingerprint device,
        /** absent means a dated event, which is what every event was before slots existed */
        SchedulingMode schedulingMode,
        @Positive Integer minAttendees,
        LocalDateTime votingDeadline,
        @Valid List<SlotRequest> slots
) {

    public SchedulingMode mode() {
        return schedulingMode == null ? SchedulingMode.FIXED : schedulingMode;
    }

    public List<SlotRequest> slotsOrEmpty() {
        return slots == null ? List.of() : slots;
    }
}
