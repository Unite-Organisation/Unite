package com.app.prod.messaging.service;

import com.app.prod.messaging.dto.CreateMessageRequest;
import com.app.prod.messaging.repository.MessageRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.MessageRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final Clock clock;
    private final Validate validate;

    public String createMessage(CreateMessageRequest request) {
        validate.user(request.senderId());
        validate.conversation(request.conversationId());

        LocalDateTime now = LocalDateTime.now(clock);

        MessageRecord record = new MessageRecord();
        record.setSenderId(request.senderId());
        record.setConversationId(request.conversationId());
        record.setSendAt(now);
        record.setContent(request.content());

        messageRepository.insertOne(record);

        String response = String.format("Created one message by %s, on %s", request.senderId(), now);
        log.info(response);
        return response;
    }
}
