package com.app.prod.job.jobs;

import java.util.UUID;

public record EventDeadlineJob(UUID eventId) implements Job {

    @Override
    public void run() {
        jobRegistry.applyEventDeadlines(eventId);
    }
}
