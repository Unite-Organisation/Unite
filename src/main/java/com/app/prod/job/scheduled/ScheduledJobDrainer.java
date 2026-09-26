package com.app.prod.job.scheduled;

import com.app.prod.job.dtos.DrainResponse;
import com.app.prod.job.jobs.Job;
import com.app.prod.job.service.JobErrorService;
import com.app.prod.utils.json.JsonbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.ScheduledJobRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledJobDrainer {
    static final int BATCH_SIZE = 50;

    private final ScheduledJobService scheduledJobService;
    private final ScheduledJobExecutor scheduledJobExecutor;
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

        try {
            scheduledJobExecutor.run(job);
            scheduledJobService.markSuccess(record.getId(), now);
            return true;
        } catch (Exception e) {
            log.error("Scheduled job {} ({}) failed", record.getJobName(), record.getId(), e);
            scheduledJobService.markFailed(record.getId(), e.getMessage(), now);
            jobErrorService.logError(job, record.getJobName(), e.getMessage(), now);
            return false;
        }
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
