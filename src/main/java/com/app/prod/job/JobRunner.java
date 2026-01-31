package com.app.prod.job;

import com.app.prod.job.enums.JobStatus;
import com.app.prod.job.jobs.Job;
import com.app.prod.job.repository.JobErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobRunner {
    private final Clock clock;
    private final JobErrorRepository jobErrorRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(JobContext jobContext) {
        Job job = jobContext.job();
        LocalDateTime now = LocalDateTime.now(clock);

        try {
            log.info("Executing job: {} at {}", job.getClass().getSimpleName(), now);
            job.run();
            jobErrorRepository.updateRerun(jobContext.jobErrorId(), JobStatus.SUCCESS, now);
            log.info("Job finished successfully");
        } catch (Exception e) {
            log.error("Job {} failed", job.getClass().getSimpleName(), e);
            jobErrorRepository.updateRerun(jobContext.jobErrorId(), JobStatus.FAILED, now);
        }
    }
}
