package com.app.prod.job.scheduled;

import com.app.prod.job.enums.JobStatus;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.sources.tables.ScheduledJob;
import org.jooq.sources.tables.records.ScheduledJobRecord;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.SCHEDULED_JOB;

@Repository
public class ScheduledJobRepository extends BaseJooqRepository<ScheduledJob, ScheduledJobRecord, UUID> {
    static final int MAX_ATTEMPTS = 3;

    protected ScheduledJobRepository(DSLContext dsl) {
        super(dsl, SCHEDULED_JOB, SCHEDULED_JOB.ID);
    }

    /**
     * Takes up to {@code batchSize} jobs that are due and marks them {@code RUNNING} in one
     * statement, so two drains running at once never get the same row: {@code SKIP LOCKED} makes
     * the second one walk past what the first is holding instead of waiting for it.
     * <p>
     * The caller must commit before running anything - the lease only protects a job while the
     * claim is visible to other instances.
     */
    public List<ScheduledJobRecord> claimDue(LocalDateTime now, Duration lease, int batchSize) {
        return dslContext.update(SCHEDULED_JOB)
                .set(SCHEDULED_JOB.STATUS, JobStatus.RUNNING.name())
                .set(SCHEDULED_JOB.LOCKED_UNTIL, now.plus(lease))
                .set(SCHEDULED_JOB.ATTEMPTS, SCHEDULED_JOB.ATTEMPTS.plus(1))
                .set(SCHEDULED_JOB.UPDATED_AT, now)
                .where(SCHEDULED_JOB.ID.in(
                        dslContext.select(SCHEDULED_JOB.ID)
                                .from(SCHEDULED_JOB)
                                .where(due(now))
                                .orderBy(SCHEDULED_JOB.RUN_AT)
                                .limit(batchSize)
                                .forUpdate()
                                .skipLocked()
                ))
                .returning()
                .fetch();
    }

    private static Condition due(LocalDateTime now) {
        return SCHEDULED_JOB.STATUS.eq(JobStatus.PENDING.name())
                .and(SCHEDULED_JOB.RUN_AT.le(now))
                .or(SCHEDULED_JOB.STATUS.eq(JobStatus.RUNNING.name())
                        .and(SCHEDULED_JOB.LOCKED_UNTIL.le(now))
                        .and(SCHEDULED_JOB.ATTEMPTS.lt(MAX_ATTEMPTS)));
    }

    public void finish(UUID id, JobStatus status, String error, LocalDateTime now) {
        dslContext.update(SCHEDULED_JOB)
                .set(SCHEDULED_JOB.STATUS, status.name())
                .set(SCHEDULED_JOB.LAST_ERROR, error)
                .set(SCHEDULED_JOB.LOCKED_UNTIL, (LocalDateTime) null)
                .set(SCHEDULED_JOB.UPDATED_AT, now)
                .where(SCHEDULED_JOB.ID.eq(id))
                .execute();
    }

    public int cancelOpen(String dedupeKey, LocalDateTime now) {
        return dslContext.update(SCHEDULED_JOB)
                .set(SCHEDULED_JOB.STATUS, JobStatus.CANCELLED.name())
                .set(SCHEDULED_JOB.LOCKED_UNTIL, (LocalDateTime) null)
                .set(SCHEDULED_JOB.UPDATED_AT, now)
                .where(SCHEDULED_JOB.DEDUPE_KEY.eq(dedupeKey))
                .and(SCHEDULED_JOB.STATUS.in(JobStatus.PENDING.name(), JobStatus.RUNNING.name()))
                .execute();
    }
}
