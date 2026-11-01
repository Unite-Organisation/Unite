package com.app.prod.job.jobs;

import com.app.prod.job.JobRegistry;

import java.util.UUID;

public record EventReturnCodeMailJob(
        UUID memberId,
        String returnCode
) implements Job {

    @Override
    public void run(JobRegistry jobRegistry) {
        jobRegistry.sendEventReturnCodeMail(this);
    }
}
