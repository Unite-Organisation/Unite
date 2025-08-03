package com.app.prod.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        UUID role
) {
}
