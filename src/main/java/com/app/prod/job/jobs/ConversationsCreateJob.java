package com.app.prod.job.jobs;

import com.app.prod.job.JobRegistry;
import com.app.prod.internal.dtos.ConversationBulkActionDto;


public record ConversationsCreateJob(
        ConversationBulkActionDto dto
) implements Job {

    @Override
    public void run(JobRegistry jobRegistry) {
        jobRegistry.createConversations(dto);
    }

}
