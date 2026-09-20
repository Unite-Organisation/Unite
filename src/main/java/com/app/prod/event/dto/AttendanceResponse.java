package com.app.prod.event.dto;

import com.app.prod.event.enums.EventMemberStatus;

public record AttendanceResponse(
        EventMemberStatus status
) {
}
