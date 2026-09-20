package com.app.prod.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserActivateRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 20) String username,
        @NotBlank String password,
        @NotBlank @Size(max = 20) String firstName,
        @NotBlank @Size(max = 20) String lastName
) {
}
