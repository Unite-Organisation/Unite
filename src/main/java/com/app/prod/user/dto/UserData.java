package com.app.prod.user.dto;

import java.util.UUID;

public record UserData(
        UUID id,
        String firstName,
        String lastName
) {
}
