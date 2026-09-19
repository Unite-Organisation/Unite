package com.app.prod.post.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventPostUpdateRequest(
        @NotNull LocalDateTime visibleFrom,
        @NotNull LocalDateTime visibleTo
) {
}
