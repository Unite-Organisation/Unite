package com.app.prod.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateEventRequest(
        @Size(max = 256) String name,
        String description,
        @Size(max = 256) String location,
        @Size(max = 2048) String onlineUrl,
        Boolean requiresApproval,
        LocalDateTime votingDeadline,
        @Valid List<SlotRequest> addSlots
) {

    public List<SlotRequest> addSlotsOrEmpty() {
        return addSlots == null ? List.of() : addSlots;
    }
}
