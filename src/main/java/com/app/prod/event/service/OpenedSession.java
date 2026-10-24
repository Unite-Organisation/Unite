package com.app.prod.event.service;

import com.app.prod.event.dto.MemberResponse;

import java.util.UUID;

public record OpenedSession(
        boolean joined,
        UUID memberId,
        MemberResponse member,
        String returnCode,
        String sessionToken
) {
}
