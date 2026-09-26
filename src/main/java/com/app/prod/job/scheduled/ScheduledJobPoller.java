package com.app.prod.job.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty("jobs.poller.enabled")
public class ScheduledJobPoller {

    private final ScheduledJobDrainer scheduledJobDrainer;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${jobs.poller.interval-ms:30000}")
    public void poll() {
        scheduledJobDrainer.drainDue(LocalDateTime.now(clock));
    }
}
