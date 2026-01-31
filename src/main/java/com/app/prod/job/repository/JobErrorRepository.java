package com.app.prod.job.repository;

import com.app.prod.job.dtos.JobResponse;
import com.app.prod.job.enums.JobStatus;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.JobError;
import org.jooq.sources.tables.records.JobErrorRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.noCondition;
import static org.jooq.sources.tables.JobError.JOB_ERROR;

@Repository
public class JobErrorRepository extends BaseJooqRepository<JobError, JobErrorRecord, UUID> {
    protected JobErrorRepository(DSLContext dsl) {
        super(dsl, JOB_ERROR, JOB_ERROR.ID);
    }

    public List<JobErrorRecord> findFailedJobs() {
        return dslContext.selectFrom(JOB_ERROR)
                .where(JOB_ERROR.STATUS.eq(JobStatus.FAILED.name()))
                .fetch();
    }

    public List<JobResponse> findJobs(JobStatus status) {
        var statusCondition = (status != null)
                ? JOB_ERROR.STATUS.eq(status.name())
                : noCondition();

        return dslContext.selectFrom(JOB_ERROR)
                .where(statusCondition)
                .fetch(record -> new JobResponse(
                        record.get(JOB_ERROR.ID),
                        record.get(JOB_ERROR.JOB_NAME),
                        JobStatus.valueOf(record.get(JOB_ERROR.STATUS)),
                        record.get(JOB_ERROR.ERROR_MESSAGE),
                        record.get(JOB_ERROR.CREATED_AT),
                        record.get(JOB_ERROR.RERUN_AT)
                ));
    }

    public void updateRerun(UUID id, JobStatus status, LocalDateTime rerunAt) {
        dslContext.update(JOB_ERROR)
                .set(JOB_ERROR.STATUS, status.name())
                .set(JOB_ERROR.RERUN_AT, rerunAt)
                .where(JOB_ERROR.ID.eq(id))
                .execute();
    }
}
