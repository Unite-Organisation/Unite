package com.app.prod.job.jobs;

import com.app.prod.config.SpringContextHolder;
import com.app.prod.conversation.service.ConversationService;
import java.util.UUID;

public record ConversationsCreateJob(
        UUID userId,
        UUID areaId,
        UUID managerId
) implements Job {

    @Override
    public void run() {
        var service = SpringContextHolder.getBean(ConversationService.class);
        service.createConversationsWithAllMembers(userId, areaId, managerId);
    }

}
