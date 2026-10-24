package com.app.prod.job.jobs;

import com.app.prod.job.JobRegistry;
import com.app.prod.internal.dtos.UserDto;

public record SyncUserJob(
        UserDto user
) implements Job {

    @Override
    public void run(JobRegistry jobRegistry) {
        jobRegistry.syncUser(user);
    }
}
