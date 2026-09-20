package com.app.prod.event.service;

import com.app.prod.event.dto.MemberResponse;

public record OpenedSession(
        boolean joined,
        MemberResponse member,
        String returnCode,
        String sessionToken
) {
}
