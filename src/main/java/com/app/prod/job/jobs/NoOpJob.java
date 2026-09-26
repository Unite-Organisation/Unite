package com.app.prod.job.jobs;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record NoOpJob(String note) implements Job {

    @Override
    public void run() {
        log.info("NoOpJob ran: {}", note);
    }
}
