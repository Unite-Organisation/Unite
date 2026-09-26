package com.app.prod.job.scheduled;

import com.app.prod.job.enums.JobStatus;
import com.app.prod.job.jobs.Job;
import com.app.prod.utils.json.JsonbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.ScheduledJobRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledJobService {
    static final Duration LEASE = Duration.ofMinutes(5);

    private final ScheduledJobRepository scheduledJobRepository;
    private final JsonbService jsonbService;
    private final Clock clock;

    /**
     * @param dedupeKey what the job is about, e.g. {@code event_voting_deadline:<eventId>}. Anything
     *                  still open under the same key is called off first, so rescheduling replaces.
     *                  {@code null} for work that may legitimately queue up more than once.
     */
    @Transactional
    public UUID schedule(Job job, LocalDateTime runAt, String dedupeKey) {
        LocalDateTime now = LocalDateTime.now(clock);
        String jobName = job.getClass().getSimpleName();

        if (dedupeKey != null) {
            int replaced = scheduledJobRepository.cancelOpen(dedupeKey, now);
            if (replaced > 0) {
                log.info("Replaced {} open job(s) under key {}", replaced, dedupeKey);
            }
        }

        ScheduledJobRecord record = new ScheduledJobRecord();
        record.setId(UUID.randomUUID());
        record.setJobName(jobName);
        record.setPayload(jsonbService.toJsonb(job));
        record.setRunAt(runAt);
        record.setStatus(JobStatus.PENDING.name());
        record.setAttempts(0);
        record.setDedupeKey(dedupeKey);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        scheduledJobRepository.insertOne(record);

        log.info("Scheduled {} for {} (key {})", jobName, runAt, dedupeKey);
        return record.getId();
    }

    @Transactional
    public void cancel(String dedupeKey) {
        int cancelled = scheduledJobRepository.cancelOpen(dedupeKey, LocalDateTime.now(clock));
        if (cancelled > 0) {
            log.info("Cancelled {} job(s) under key {}", cancelled, dedupeKey);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ScheduledJobRecord> claim(LocalDateTime now, int batchSize) {
        return scheduledJobRepository.claimDue(now, LEASE, batchSize);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(UUID id, LocalDateTime now) {
        scheduledJobRepository.finish(id, JobStatus.SUCCESS, null, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID id, String error, LocalDateTime now) {
        scheduledJobRepository.finish(id, JobStatus.FAILED, error, now);
    }
}
