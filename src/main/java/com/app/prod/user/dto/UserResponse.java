package com.app.prod.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String password,
        String role,
        LocalDateTime created_at
) {
}
