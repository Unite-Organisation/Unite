package com.app.prod.conversation.mappers;

import com.app.prod.conversation.dto.ConversationRequest;
import com.app.prod.conversation.dto.ConversationResponse;
import org.jooq.sources.tables.records.ConversationsRecord;
import org.jooq.sources.tables.records.UsersRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConversationMapper {

    public static ConversationResponse fromRecordToResponse(ConversationsRecord conversationsRecord){
        return new ConversationResponse(
                conversationsRecord.getId(),
                conversationsRecord.getIsGroup(),
                conversationsRecord.getName(),
                conversationsRecord.getCreatedAt()
        );
    }

    public static List<ConversationResponse> fromRecordsToResponses(List<ConversationsRecord> conversations){
        List<ConversationResponse> response = new ArrayList<>();
        for(var conversation : conversations){
            response.add(fromRecordToResponse(conversation));
        }
        return response;
    }

    public static ConversationsRecord fromRequestToRecord(ConversationRequest request, UUID id, LocalDateTime now) {
        return new ConversationsRecord(
                id,
                request.isGroup(),
                request.name(),
                now
        );
    }
}
