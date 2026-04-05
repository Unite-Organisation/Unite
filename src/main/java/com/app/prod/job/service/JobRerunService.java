package com.app.prod.job.service;

import com.app.prod.job.JobContext;
import com.app.prod.job.JobRunner;
import com.app.prod.job.enums.JobStatus;
import com.app.prod.job.jobs.Job;
import com.app.prod.job.repository.JobErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.JobErrorRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobRerunService {

    private final JobErrorRepository jobErrorRepository;
    private final JobRunner jobRunner;
    private final JobErrorService jobErrorService;
    private final Clock clock;

    public JobStatus rerunJob(JobContext jobContext) {
        return jobRunner.execute(jobContext);
    }

    public JobStatus rerunJob(UUID errorId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Job job = getJob(errorId);
        if (job == null) {
            log.warn("Failed to deserialize job from error: {}", errorId);
            throw new RuntimeException("Failed to deserialize job from error");
        }
        return rerunJob(new JobContext(job, errorId));
    }

    public List<JobContext> getFailedJobs() {
        List<JobErrorRecord> failedJobs = jobErrorRepository.findFailedJobs();
        return failedJobs.stream()
                .map(error -> new JobContext(jobErrorService.deserializeJon(error.getPayload()), error.getId()))
                .filter(Objects::nonNull)
                .toList();
    }

    public Job getJob(UUID errorId) {
        JobErrorRecord error = jobErrorRepository.findById(errorId).orElseThrow(
                () -> new IllegalArgumentException("Error not found"));

        Job job = jobErrorService.deserializeJon(error.getPayload());
        if (job == null) {
            log.warn("Failed to deserialize job from error: {}", errorId);
        }
        return job;
    }

    public void rerunJobs(List<JobContext> jobs) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            jobs.forEach(record -> executor.submit(() -> {
                jobRunner.execute(record);
            }));
        }
    }

}
