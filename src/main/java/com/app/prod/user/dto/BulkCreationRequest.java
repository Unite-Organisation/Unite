package com.app.prod.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkCreationRequest(
        @NotEmpty
        @Size(max = 1000)
        List<@NotEmpty @Email @Size(max = 50) String> emails
) {
}
