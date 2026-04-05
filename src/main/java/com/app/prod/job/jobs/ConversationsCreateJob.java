package com.app.prod.job.jobs;

import com.app.prod.internal.dtos.ConversationBulkActionDto;


public record ConversationsCreateJob(
        ConversationBulkActionDto dto
) implements Job {

    @Override
    public void run() {
        jobRegistry.createConversations(dto);
    }

}
