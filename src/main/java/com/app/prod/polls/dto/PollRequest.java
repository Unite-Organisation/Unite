package com.app.prod.polls.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PollRequest(
        @NotNull @NotEmpty String title,
        String description,
        UUID buildingId,
        @NotNull boolean anonymous,
        LocalDateTime startTime,
        LocalDateTime endTime,

        @Size(max = 10, message = "Options limit is set to 10")
        List<String> options
) {
}



