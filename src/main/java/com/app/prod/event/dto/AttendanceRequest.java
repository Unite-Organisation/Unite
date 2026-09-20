package com.app.prod.event.dto;

import com.app.prod.event.enums.EventMemberStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceRequest(
        @NotNull EventMemberStatus status
) {
}
