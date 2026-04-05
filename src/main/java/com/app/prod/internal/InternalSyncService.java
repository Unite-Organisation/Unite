package com.app.prod.internal;

import com.app.prod.internal.clients.ChattingServiceClient;
import com.app.prod.internal.dtos.ConversationBulkActionDto;
import com.app.prod.internal.dtos.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternalSyncService {

    private final ChattingServiceClient chattingServiceClient;

    public void syncUser(UserDto user) {
        chattingServiceClient.syncUser(user);
    }

    public void createConversations(ConversationBulkActionDto dto) {
        chattingServiceClient.createConversationsForNewUser(dto);
    }

}
