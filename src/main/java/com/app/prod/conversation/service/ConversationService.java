package com.app.prod.conversation.service;

import com.app.prod.conversation.dto.AddMembersRequest;
import com.app.prod.conversation.dto.ConversationRequest;
import com.app.prod.conversation.mappers.ConversationMapper;
import com.app.prod.conversation.repository.ConversationMemberRepository;
import com.app.prod.conversation.repository.ConversationRepository;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.messaging.repository.MessageRepository;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.ConversationMemberRecord;
import org.jooq.sources.tables.records.ConversationsRecord;
import org.jooq.sources.tables.records.MessageRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final Clock clock;
    private final Validate validate;

    public List<ConversationsRecord> getConversations() {
        return conversationRepository.findAll();
    }

    public void createConversation(ConversationRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        conversationRepository.insertOne(ConversationMapper.fromRequestToRecord(request, id, now));
    }

    @Transactional
    public void addMembersToConversation(AddMembersRequest request) {

        UUID conversationId = request.conversationId();
        validate.conversation(conversationId);

        List<ConversationMemberRecord> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);

        for(var memberId : request.ids()){
            validate.user(memberId);

            UUID id = UUID.randomUUID();
            batch.add(new ConversationMemberRecord(
                    id,
                    memberId,
                    conversationId,
                    now
            ));
        }

        log.info("Adding {} users into conversation with id: {}", batch.size(), conversationId);
        conversationMemberRepository.insertMany(batch);
    }

    public List<MessageRecord> getConversationContent(UUID conversationid) {
        validate.conversation(conversationid);
        List<MessageRecord> messages = messageRepository.findByConversationId(conversationid);

        log.info("Fetched {} messages in conversation: {}", messages.size(), conversationid);
        return messages;
    }
}
