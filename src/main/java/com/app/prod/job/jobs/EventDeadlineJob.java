package com.app.prod.job.jobs;

import com.app.prod.job.JobRegistry;
import java.util.UUID;

public record EventDeadlineJob(UUID eventId) implements Job {

    @Override
    public void run(JobRegistry jobRegistry) {
        jobRegistry.applyEventDeadlines(eventId);
    }
}
