package com.app.prod.job.scheduled;

import com.app.prod.job.jobs.Job;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduledJobExecutor {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void run(Job job) {
        job.run();
    }
}
