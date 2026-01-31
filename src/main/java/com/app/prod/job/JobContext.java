package com.app.prod.job;

import com.app.prod.job.jobs.Job;

import java.util.UUID;

public record JobContext(
        Job job,
        UUID jobErrorId
) {
}
