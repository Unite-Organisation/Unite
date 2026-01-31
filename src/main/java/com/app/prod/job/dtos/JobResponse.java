package com.app.prod.job.dtos;

import com.app.prod.job.enums.JobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String jobName,
        JobStatus status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime rerunAt
) {
}
