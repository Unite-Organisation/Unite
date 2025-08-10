package com.app.prod.user.dto;

import java.util.UUID;

public record UserRegisterRequest(
        String firstName,
        String lastName,
        String username,
        String email,
        String password,
        UUID role
) {
}
