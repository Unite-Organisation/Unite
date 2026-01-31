package com.app.prod.services.schedulers;

import com.app.prod.job.JobContext;
import com.app.prod.job.service.JobRerunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FailedJobsScheduler {

    private final Clock clock;
    private final JobRerunService jobRerunService;

    @Scheduled(cron = "0 0 1 * * *")
    public void rerunFailedJobs() {
        log.info("Rerunning failed jobs at {}", LocalDateTime.now(clock));
        List<JobContext> failedJobs = jobRerunService.getFailedJobs();
        if (failedJobs.isEmpty()) {
            log.info("No failed jobs to rerun");
            return;
        }
        jobRerunService.rerunJobs(failedJobs);
        log.info("Finished rerunning jobs");
    }

}
