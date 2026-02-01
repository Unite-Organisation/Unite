package com.app.prod.job.jobs;

import java.util.UUID;

public record ConversationsCreateJob(
        UUID userId,
        UUID areaId,
        UUID managerId
) implements Job {

    @Override
    public void run() {
        jobRegistry.conversationCreateJob(userId, areaId, managerId);
    }

}
