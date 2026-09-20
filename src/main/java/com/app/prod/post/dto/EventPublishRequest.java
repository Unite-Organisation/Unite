package com.app.prod.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record EventPublishRequest(
        @NotBlank String slug,
        @NotNull LocalDateTime visibleFrom,
        @NotNull LocalDateTime visibleTo,
        @Size(max = 5)
        List<String> fileKeys
) {
}
