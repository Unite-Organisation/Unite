package com.app.prod.user.dto;

import com.app.prod.user.enums.SkipReason;

public record SkippedEmail(
        String email,
        SkipReason reason
) {
}
