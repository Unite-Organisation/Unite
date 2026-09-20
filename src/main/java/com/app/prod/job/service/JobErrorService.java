package com.app.prod.job.service;

import com.app.prod.job.jobs.Job;
import com.app.prod.job.dtos.JobResponse;
import com.app.prod.job.repository.JobErrorRepository;
import com.app.prod.job.enums.JobStatus;
import com.app.prod.utils.json.JsonbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.JSONB;
import org.jooq.sources.tables.records.JobErrorRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobErrorService {

    private final JobErrorRepository jobErrorRepository;
    private final JsonbService jsonbService;

    public void logError(Job job, String jobName, String errorMessage, LocalDateTime failedAt) {
        JSONB jsonb = serializeJob(job);

        if (jsonb == null) {
            log.error("Failed to serialize job: {}", jobName);
            return;
        }

        var record = new JobErrorRecord(
                UUID.randomUUID(),
                jobName,
                jsonb,
                JobStatus.FAILED.name(),
                errorMessage,
                failedAt,
                failedAt
        );
        jobErrorRepository.insertOne(record);
        log.info("Logged error for job: {}", jobName);
    }

    public JSONB serializeJob(Job job) {
        try {
            return jsonbService.toJsonb(job);
        } catch (Exception e) {
            return null;
        }
    }

    public Job deserializeJon(JSONB jsonb)  {
        try {
            return jsonbService.fromJsonb(jsonb, Job.class);
        } catch (Exception e) {
            return null;
        }
    }

    public List<JobResponse> getJobs(JobStatus status) {
        return jobErrorRepository.findJobs(status);
    }
}
