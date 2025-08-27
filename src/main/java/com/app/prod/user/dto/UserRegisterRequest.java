package com.app.prod.user.dto;

import com.app.prod.user.enums.UserRole;

import java.util.UUID;

public record UserRegisterRequest(
        String firstName,
        String lastName,
        String username,
        String email,
        String password,
        UserRole role
) {
}
