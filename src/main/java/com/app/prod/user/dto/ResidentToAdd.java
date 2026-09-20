package com.app.prod.user.dto;

import java.util.UUID;

public record ResidentToAdd(
        String firstName,
        String lastName,
        UUID userId,
        String username,
        String email
) {
}
