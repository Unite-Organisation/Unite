package com.app.prod.job.jobs;

import com.app.prod.job.JobRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record NoOpJob(String note) implements Job {

    @Override
    public void run(JobRegistry jobRegistry) {
        log.info("NoOpJob ran: {}", note);
    }
}
