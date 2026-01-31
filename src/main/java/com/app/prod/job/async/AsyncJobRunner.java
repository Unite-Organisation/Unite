package com.app.prod.job.async;

import com.app.prod.job.jobs.Job;
import com.app.prod.job.service.JobErrorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncJobRunner {

    private final JobErrorService jobErrorService;
    private final Clock clock;

    @Async("taskExecutor")
    @Transactional
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void execute(Job job) {
        LocalDateTime now = LocalDateTime.now(clock);
        String jobName = job.getClass().getSimpleName();
        log.info("Running job: {} at {}", jobName, now);
        try {
            job.run();
            log.info("Job {} finished successfully", jobName);
        } catch (Exception e) {
            log.error("Job {} failed", jobName, e);
            throw e;
        }
    }

    @Recover
    public void globalRecover(Exception e, Job job) {
        LocalDateTime now = LocalDateTime.now(clock);
        String jobName = job.getClass().getSimpleName();
        log.error("Running job {} failed after 3 retries.", jobName);
        jobErrorService.logError(job, jobName, e.getMessage(), now);
    }

}
