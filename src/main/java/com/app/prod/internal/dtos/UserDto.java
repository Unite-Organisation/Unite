package com.app.prod.internal.dtos;

import com.app.prod.user.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String username,
        String password,
        UUID userRole,
        UserStatus status,
        LocalDateTime createdAt
) {
}
