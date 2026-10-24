package com.app.prod.job.scheduled;

import com.app.prod.job.JobRegistry;
import com.app.prod.job.dtos.DrainResponse;
import com.app.prod.job.jobs.Job;
import com.app.prod.job.service.JobErrorService;
import com.app.prod.utils.json.JsonbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.ScheduledJobRecord;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledJobDrainer {
    static final int BATCH_SIZE = 50;
    /** the n-th failed attempt waits n times this long - SMTP or a downstream that is down is usually down for minutes */
    static final Duration RETRY_DELAY = Duration.ofMinutes(5);

    private final ScheduledJobService scheduledJobService;
    private final JobRegistry jobRegistry;
    private final JobErrorService jobErrorService;
    private final JsonbService jsonbService;

    public DrainResponse drainDue(LocalDateTime now) {
        List<ScheduledJobRecord> claimed = scheduledJobService.claim(now, BATCH_SIZE);
        if (claimed.isEmpty()) {
            return DrainResponse.empty();
        }

        log.info("Draining {} scheduled job(s)", claimed.size());
        int succeeded = 0;
        for (ScheduledJobRecord record : claimed) {
            if (execute(record, now)) {
                succeeded++;
            }
        }

        DrainResponse response = new DrainResponse(claimed.size(), succeeded, claimed.size() - succeeded);
        log.info("Drain finished: {}", response);
        return response;
    }

    private boolean execute(ScheduledJobRecord record, LocalDateTime now) {
        Job job = deserialize(record, now);
        if (job == null) {
            return false;
        }

        // no transaction around the job: each one opens what it needs. A job that talks to SMTP
        // must not hold a connection for it, and what it already did - a mail that went out - must
        // survive the job failing on the next one, or the retry would do it twice
        try {
            job.run(jobRegistry);
            scheduledJobService.markSuccess(record.getId(), now);
            return true;
        } catch (Exception e) {
            retryOrFail(record, job, e, now);
            return false;
        }
    }

    private void retryOrFail(ScheduledJobRecord record, Job job, Exception e, LocalDateTime now) {
        if (record.getAttempts() < ScheduledJobRepository.MAX_ATTEMPTS) {
            LocalDateTime retryAt = now.plus(RETRY_DELAY.multipliedBy(record.getAttempts()));
            log.warn("Scheduled job {} ({}) failed on attempt {}, retrying at {}", record.getJobName(), record.getId(), record.getAttempts(), retryAt, e);
            scheduledJobService.retryAt(record.getId(), retryAt, e.getMessage(), now);
            return;
        }

        log.error("Scheduled job {} ({}) failed on its last attempt", record.getJobName(), record.getId(), e);
        scheduledJobService.markFailed(record.getId(), e.getMessage(), now);
        jobErrorService.logError(job, record.getJobName(), e.getMessage(), now);
    }

    private Job deserialize(ScheduledJobRecord record, LocalDateTime now) {
        try {
            Job job = jsonbService.fromJsonb(record.getPayload(), Job.class);
            if (job == null) {
                scheduledJobService.markFailed(record.getId(), "payload is empty", now);
            }
            return job;
        } catch (Exception e) {
            log.error("Could not read payload of scheduled job {} ({})", record.getJobName(), record.getId(), e);
            scheduledJobService.markFailed(record.getId(), "payload could not be read: " + e.getMessage(), now);
            return null;
        }
    }
}
