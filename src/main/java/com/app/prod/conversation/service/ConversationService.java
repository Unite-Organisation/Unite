package com.app.prod.conversation.service;

import com.app.prod.conversation.dto.*;
import com.app.prod.conversation.mappers.ConversationMapper;
import com.app.prod.conversation.repository.ConversationMemberRepository;
import com.app.prod.conversation.repository.ConversationRepository;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.messaging.repository.MessageRepository;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.service.UserService;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.shared.EntityCreatedResponse;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.Conversation;
import org.jooq.sources.tables.Request;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.ConversationMemberRecord;
import org.jooq.sources.tables.records.ConversationRecord;
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
    private final UserService userService;

    public List<ConversationResponse> getConversations(AppUserRecord user, Pagination pagination) {
        List<ConversationResponse> conversations = conversationRepository.findConversationForUser(user.getId(), pagination);
        log.info("Found {} conversations for user {}", conversations.size(), user.getId());
        return conversations;
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
            throw new BadRequestException("You have already this private chat");
        }
    }

    public List<ConversationMessageResponse> getConversationContent(UUID conversationId, Pagination pagination) {
        validate.conversation(conversationId);
        List<ConversationMessageResponse> messages = messageRepository.findByConversationId(conversationId, pagination);
        log.info("Fetched {} messages in conversation: {}", messages.size(), conversationId);
        return messages;
    }

    public UUID getConversationId(UUID user1Id, UUID user2Id){
        return conversationRepository.findDirectConversationForUsers(user1Id, user2Id)
                .orElseThrow(() -> new EntityNotPresentException(
                        String.format("Conversation for %s and %s not found", user1Id, user2Id),
                        Conversation.class.getSimpleName()
                ));

    }

    public void createConversationsWithAllMembers(UUID userId, UUID areaId, UUID managerId){
        List<UUID> usersInArea = userService.getAllUsersInAreaWithoutUser(userId, areaId);

        List<ConversationRecord> conversationRecords = new ArrayList<>();
        List<ConversationMemberRecord> conversationMemberRecords = new ArrayList<>();
        var now = LocalDateTime.now(clock);

        // conversation with other users
        for(UUID contactId : usersInArea){
            var conversationId = UUID.randomUUID();

            conversationRecords.add(new ConversationRecord(conversationId, false, null, now, now));

            conversationMemberRecords.add(new ConversationMemberRecord(UUID.randomUUID(), userId, conversationId, now));
            conversationMemberRecords.add(new ConversationMemberRecord(UUID.randomUUID(), contactId, conversationId, now));

        }

        // conversation with manager
        var conversationId = UUID.randomUUID();
        conversationRecords.add(new ConversationRecord(conversationId, false, null, now, now));
        conversationMemberRecords.add(new ConversationMemberRecord(UUID.randomUUID(), userId, conversationId, now));
        conversationMemberRecords.add(new ConversationMemberRecord(UUID.randomUUID(), managerId, conversationId, now));

        conversationRepository.insertMany(conversationRecords);
        conversationMemberRepository.insertMany(conversationMemberRecords);
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
