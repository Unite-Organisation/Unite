package com.app.prod.user.dto;

import java.util.UUID;

public record CreatedAccount(
        UUID userId,
        String email
) {
}
