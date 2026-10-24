package com.app.prod.job;

import com.app.prod.event.service.EventLifecycleService;
import com.app.prod.internal.InternalSyncService;
import com.app.prod.internal.dtos.ConversationBulkActionDto;
import com.app.prod.internal.dtos.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobRegistry {
    private final InternalSyncService internalSyncService;
    private final EventLifecycleService eventLifecycleService;
    private final Clock clock;

    public void syncUser(UserDto user) {
        internalSyncService.syncUser(user);
    }

    public void createConversations(ConversationBulkActionDto dto) {
        internalSyncService.createConversations(dto);
    }

    public void applyEventDeadlines(UUID eventId) { eventLifecycleService.applyPassedDeadlines(eventId, LocalDateTime.now(clock)); }

}
