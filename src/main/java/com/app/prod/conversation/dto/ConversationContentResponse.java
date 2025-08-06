package com.app.prod.conversation.dto;

import org.jooq.sources.tables.records.MessageRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ConversationContentResponse(
        List<ConversationMessage> conversationMessages
) {
    private record ConversationMessage(
            UUID author,
            LocalDateTime sendAt,
            String content
    ){}

    public static ConversationContentResponse fromListOfMessagesToResponse(List<MessageRecord> records){
        List<ConversationMessage> messages = new ArrayList<>();
        for(var record : records){
            messages.add(new ConversationMessage(
                    record.getSenderId(),
                    record.getSendAt(),
                    record.getContent()
            ));
        }
        return new ConversationContentResponse(messages);
    }
}
