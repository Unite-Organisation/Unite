package com.app.prod.event.dto;

public record EventSessionResponse(
        MemberResponse member,
        String returnCode
) {
}
