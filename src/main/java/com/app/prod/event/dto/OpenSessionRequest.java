package com.app.prod.event.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OpenSessionRequest(
        @Size(max = 60) String displayName,
        @Pattern(regexp = "\\d{4}") String returnCode
) {
}
