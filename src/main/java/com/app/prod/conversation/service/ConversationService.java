package com.app.prod.conversation.service;

import com.app.prod.conversation.dto.AddMembersRequest;
import com.app.prod.conversation.dto.ConversationRequest;
import com.app.prod.conversation.mappers.ConversationMapper;
import com.app.prod.conversation.repository.ConversationMemberRepository;
import com.app.prod.conversation.repository.ConversationRepository;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.ConversationMemberRecord;
import org.jooq.sources.tables.records.ConversationsRecord;
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
    private final UserRepository userRepository;
    private final Clock clock;

    public List<ConversationsRecord> getConversations() {
        return conversationRepository.findAll();
    }

    public void createConversation(ConversationRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        conversationRepository.insert(ConversationMapper.fromRequestToRecord(request, id, now));
    }

    @Transactional
    public void addMembersToConversation(AddMembersRequest request) {

        UUID conversationId = request.conversationId();
        if(!conversationRepository.exists(conversationId)){
            throw new EntityNotPresentException(String.format("Conversation with id: %s doesn't exist.", conversationId));
        }

        List<ConversationMemberRecord> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);

        for(var memberId : request.ids()){
            if(!userRepository.exists(memberId)){
                throw new EntityNotPresentException(String.format("User with id: %s doesn't exist.", memberId));
            }

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
}
