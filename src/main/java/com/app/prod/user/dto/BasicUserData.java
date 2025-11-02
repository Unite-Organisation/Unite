package com.app.prod.user.dto;

import java.util.UUID;

public record BasicUserData(
        UUID id,
        String firstName,
        String lastName,
        String role
) {
}
