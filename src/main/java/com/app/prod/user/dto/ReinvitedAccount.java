package com.app.prod.user.dto;

import java.util.UUID;

public record ReinvitedAccount(
        UUID userId,
        String email
) {
}
