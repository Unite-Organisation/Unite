package com.app.prod.job.service;

import com.app.prod.job.jobs.Job;
import com.app.prod.job.dtos.JobResponse;
import com.app.prod.job.repository.JobErrorRepository;
import com.app.prod.job.enums.JobStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public void logError(Job job, String jobName, String errorMessage, LocalDateTime failedAt) {
        String serializedJob = serializeJob(job);

        if (serializedJob == null) {
            log.error("Failed to serialize job: {}", jobName);
            return;
        }

        JSONB jsonb = JSONB.valueOf(serializedJob);
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

    public String serializeJob(Job job) {
        try {
            return objectMapper.writeValueAsString(job);
        } catch (Exception e) {
            return null;
        }
    }

    public Job deserializeJon(JSONB jsonb)  {
        try {
            return objectMapper.readValue(jsonb.data(), Job.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public List<JobResponse> getJobs(JobStatus status) {
        return jobErrorRepository.findJobs(status);
    }
}
