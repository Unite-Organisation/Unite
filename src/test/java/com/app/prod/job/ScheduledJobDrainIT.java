package com.app.prod.job;

import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.job.dtos.DrainResponse;
import com.app.prod.job.enums.JobStatus;
import com.app.prod.job.jobs.NoOpJob;
import com.app.prod.job.scheduled.ScheduledJobDrainer;
import com.app.prod.job.scheduled.ScheduledJobService;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.sources.tables.records.ScheduledJobRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.sources.Tables.JOB_ERROR;
import static org.jooq.sources.Tables.SCHEDULED_JOB;

@SpringBootTest
class ScheduledJobDrainIT extends IntegrationTest {
    private static final Duration LEASE = Duration.ofMinutes(5);

    @Autowired
    private ScheduledJobService scheduledJobService;
    @Autowired
    private ScheduledJobDrainer scheduledJobDrainer;
    @Autowired
    private DSLContext dslContext;
    @Autowired
    private MutableClock clock;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(SCHEDULED_JOB).execute();
        dslContext.deleteFrom(JOB_ERROR).execute();
        now = LocalDateTime.now(clock);
    }

    @Test
    void shouldLeaveAJobThatIsNotDueYetAlone() {
        UUID id = scheduledJobService.schedule(new NoOpJob("later"), now.plusHours(1), null);

        DrainResponse response = scheduledJobDrainer.drainDue(now);

        assertThat(response).isEqualTo(DrainResponse.empty());
        assertThat(statusOf(id)).isEqualTo(JobStatus.PENDING);
        assertThat(recordOf(id).getAttempts()).isZero();
    }

    @Test
    void shouldRunADueJobAndMarkItSuccessful() {
        UUID id = scheduledJobService.schedule(new NoOpJob("now"), now.minusMinutes(1), null);

        DrainResponse response = scheduledJobDrainer.drainDue(now);

        assertThat(response).isEqualTo(new DrainResponse(1, 1, 0));

        ScheduledJobRecord finished = recordOf(id);
        assertThat(JobStatus.valueOf(finished.getStatus())).isEqualTo(JobStatus.SUCCESS);
        assertThat(finished.getAttempts()).isEqualTo(1);
        assertThat(finished.getLockedUntil()).isNull();
        assertThat(finished.getLastError()).isNull();
    }

    @Test
    void shouldReplaceAnOpenJobScheduledUnderTheSameKey() {
        String key = "event_voting_deadline:" + UUID.randomUUID();

        UUID first = scheduledJobService.schedule(new NoOpJob("first"), now.plusHours(1), key);
        UUID second = scheduledJobService.schedule(new NoOpJob("second"), now.plusHours(2), key);

        assertThat(statusOf(first)).isEqualTo(JobStatus.CANCELLED);
        assertThat(statusOf(second)).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void shouldNotRunAJobThatWasCalledOff() {
        String key = "event_voting_deadline:" + UUID.randomUUID();
        UUID id = scheduledJobService.schedule(new NoOpJob("dropped"), now.minusMinutes(1), key);

        scheduledJobService.cancel(key);
        DrainResponse response = scheduledJobDrainer.drainDue(now);

        assertThat(response).isEqualTo(DrainResponse.empty());
        assertThat(statusOf(id)).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void shouldHandOneDueJobToOnlyOneOfTwoDrainsRunningAtOnce() throws Exception {
        scheduledJobService.schedule(new NoOpJob("contested"), now.minusMinutes(1), null);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        Callable<Integer> claim = () -> {
            ready.countDown();
            go.await();
            return scheduledJobService.claim(now, 10).size();
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(claim);
            Future<Integer> second = executor.submit(claim);
            ready.await();
            go.countDown();

            assertThat(first.get() + second.get())
                    .as("SKIP LOCKED must hand the row to exactly one drain, never to both")
                    .isEqualTo(1);
        }
    }

    @Test
    void shouldTakeBackAJobWhoseLeaseRanOut() {
        UUID id = scheduledJobService.schedule(new NoOpJob("abandoned"), now.minusMinutes(1), null);

        assertThat(scheduledJobService.claim(now, 10)).hasSize(1);
        assertThat(statusOf(id)).isEqualTo(JobStatus.RUNNING);

        // still held: the instance that took it may simply be slow
        assertThat(scheduledJobService.claim(now.plus(LEASE).minusSeconds(1), 10)).isEmpty();

        // lease gone: whoever held it is not coming back
        List<ScheduledJobRecord> reclaimed = scheduledJobService.claim(now.plus(LEASE).plusSeconds(1), 10);
        assertThat(reclaimed).hasSize(1);
        assertThat(reclaimed.getFirst().getId()).isEqualTo(id);
        assertThat(recordOf(id).getAttempts()).isEqualTo(2);
    }

    @Test
    void shouldStopTakingBackAJobThatKeepsBeingAbandoned() {
        UUID id = scheduledJobService.schedule(new NoOpJob("poison"), now.minusMinutes(1), null);

        LocalDateTime attempt = now;
        for (int i = 0; i < 3; i++) {
            assertThat(scheduledJobService.claim(attempt, 10)).hasSize(1);
            attempt = attempt.plus(LEASE).plusSeconds(1);
        }

        assertThat(scheduledJobService.claim(attempt, 10))
                .as("a job that takes its instance down with it must not be retried forever")
                .isEmpty();
        assertThat(recordOf(id).getAttempts()).isEqualTo(3);
    }

    @Test
    void shouldFailAJobWhosePayloadCannotBeRead() {
        UUID id = insertRaw("GhostJob", JSONB.valueOf("{\"@type\":\"GhostJob\"}"), now.minusMinutes(1));

        DrainResponse response = scheduledJobDrainer.drainDue(now);

        assertThat(response).isEqualTo(new DrainResponse(1, 0, 1));

        ScheduledJobRecord failed = recordOf(id);
        assertThat(JobStatus.valueOf(failed.getStatus())).isEqualTo(JobStatus.FAILED);
        assertThat(failed.getLastError()).isNotBlank();
        assertThat(failed.getLockedUntil()).isNull();
    }

    @Test
    void shouldDrainEverythingDueInOneCall() {
        scheduledJobService.schedule(new NoOpJob("a"), now.minusMinutes(3), null);
        scheduledJobService.schedule(new NoOpJob("b"), now.minusMinutes(2), null);
        scheduledJobService.schedule(new NoOpJob("c"), now.plusHours(1), null);

        DrainResponse response = scheduledJobDrainer.drainDue(now);

        assertThat(response).isEqualTo(new DrainResponse(2, 2, 0));
    }

    private UUID insertRaw(String jobName, JSONB payload, LocalDateTime runAt) {
        UUID id = UUID.randomUUID();
        dslContext.insertInto(SCHEDULED_JOB)
                .set(SCHEDULED_JOB.ID, id)
                .set(SCHEDULED_JOB.JOB_NAME, jobName)
                .set(SCHEDULED_JOB.PAYLOAD, payload)
                .set(SCHEDULED_JOB.RUN_AT, runAt)
                .set(SCHEDULED_JOB.STATUS, JobStatus.PENDING.name())
                .set(SCHEDULED_JOB.ATTEMPTS, 0)
                .set(SCHEDULED_JOB.CREATED_AT, runAt)
                .set(SCHEDULED_JOB.UPDATED_AT, runAt)
                .execute();
        return id;
    }

    private JobStatus statusOf(UUID id) {
        return JobStatus.valueOf(recordOf(id).getStatus());
    }

    private ScheduledJobRecord recordOf(UUID id) {
        return dslContext.selectFrom(SCHEDULED_JOB)
                .where(SCHEDULED_JOB.ID.eq(id))
                .fetchOne();
    }
}
