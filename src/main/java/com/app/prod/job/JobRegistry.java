package com.app.prod.job;

import com.app.prod.conversation.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobRegistry {
    private final ConversationService conversationService;

    public void conversationCreateJob(UUID userId, UUID areaId, UUID managerId) {
        conversationService.createConversationsWithAllMembers(userId, areaId, managerId);
    }

}
