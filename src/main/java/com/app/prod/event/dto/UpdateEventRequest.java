package com.app.prod.event.dto;

import jakarta.validation.constraints.Size;

public record UpdateEventRequest(
        @Size(max = 256) String name,
        String description,
        @Size(max = 256) String location,
        @Size(max = 2048) String onlineUrl,
        Boolean requiresApproval
) {
}
