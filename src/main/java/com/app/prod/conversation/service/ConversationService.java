package com.app.prod.conversation.service;

import com.app.prod.conversation.dto.*;
import com.app.prod.conversation.mappers.ConversationMapper;
import com.app.prod.conversation.repository.ConversationMemberRepository;
import com.app.prod.conversation.repository.ConversationRepository;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.messaging.repository.MessageRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.shared.EntityCreatedResponse;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.Conversation;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.ConversationMemberRecord;
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

    public List<ConversationResponse> getConversations(AppUserRecord user, Pagination pagination) {
        return conversationRepository.findConversationForUser(user.getId(), pagination);
    }

    public EntityCreatedResponse createConversation(ConversationRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        conversationRepository.insertOne(ConversationMapper.fromRequestToRecord(request, id, now));
        return new EntityCreatedResponse(id);
    }

    @Transactional
    public void addMembersToConversation(AddMembersRequest request, AppUserRecord user) {
        checkNoDoubleConversationWithTheSameContact(request.ids(), user);

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

    private void checkNoDoubleConversationWithTheSameContact(List<UUID> membersToAdd, AppUserRecord user){
        // If list of members to add is greater than one than we are creating group - not 1to1 chat
        if(membersToAdd.size() > 1 )
            return;

        if(conversationMemberRepository.existsPrivateConversationBetweenContacts(user.getId(), membersToAdd.getFirst())){
            throw new BadRequestException(AppError.of(Code.PRIVATE_CONVERSATION_EXISTS));
        }
    }

    public List<ConversationMessageResponse> getConversationContent(UUID conversationId, Pagination pagination) {
        validate.conversation(conversationId);
        return messageRepository.findByConversationId(conversationId, pagination);
    }

    public UUID getConversationId(UUID user1Id, UUID user2Id){
        return conversationRepository.findDirectConversationForUsers(user1Id, user2Id)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.CONVERSATION_NOT_FOUND)));
    }

    @Transactional
    public void createGroupConversation(GroupConversationRequest request, AppUserRecord user) {
        var conversationRequest = new ConversationRequest(true, request.name());
        var conversationId = createConversation(conversationRequest).entityId();
        var addMembersRequest = new AddMembersRequest(conversationId, request.members());
        addMembersToConversation(addMembersRequest, user);
        addUserToConversation(user.getId(), conversationId);
    }

    private void addUserToConversation(UUID userId, UUID conversationId){
        var now = LocalDateTime.now(clock);
        conversationMemberRepository.insertOne(new ConversationMemberRecord(UUID.randomUUID(), userId, conversationId, now));
    }
}
