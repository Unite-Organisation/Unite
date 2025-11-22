package com.app.prod.conversation.mappers;

import com.app.prod.conversation.dto.ConversationRequest;
import com.app.prod.conversation.dto.ConversationResponse;
import org.jooq.sources.tables.records.ConversationRecord;
import org.jooq.sources.tables.records.AppUserRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConversationMapper {

    public static ConversationResponse fromRecordToResponse(ConversationRecord conversationsRecord){
        return new ConversationResponse(
                conversationsRecord.getId(),
                conversationsRecord.getIsGroup(),
                conversationsRecord.getName(),
                conversationsRecord.getCreatedAt(),
                conversationsRecord.getUpdatedAt()
        );
    }

    public static List<ConversationResponse> fromRecordsToResponses(List<ConversationRecord> conversations){
        List<ConversationResponse> response = new ArrayList<>();
        for(var conversation : conversations){
            response.add(fromRecordToResponse(conversation));
        }
        return response;
    }

    public static ConversationRecord fromRequestToRecord(ConversationRequest request, UUID id, LocalDateTime now) {
        return new ConversationRecord(
                id,
                request.isGroup(),
                request.name(),
                now,
                now
        );
    }
}
