package com.app.prod.conversation.api;

import com.app.prod.conversation.dto.AddMembersRequest;
import com.app.prod.conversation.dto.ConversationContentResponse;
import com.app.prod.conversation.dto.ConversationRequest;
import com.app.prod.conversation.dto.ConversationResponse;
import com.app.prod.conversation.mappers.ConversationMapper;
import com.app.prod.conversation.service.ConversationService;
import com.app.prod.messaging.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.ConversationsRecord;
import org.jooq.sources.tables.records.MessageRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("conversation")
@RequiredArgsConstructor
@Deprecated
@Tag(name = "Conversations")
public class ConversationRestApi {

    private final ConversationService conversationService;

    @GetMapping()
    public ResponseEntity<List<ConversationResponse>> getConversations(){
        List<ConversationsRecord> conversations = conversationService.getConversations();
        return ResponseEntity.ok(ConversationMapper.fromRecordsToResponses(conversations));
    }

    @PostMapping
    public ResponseEntity<Void> createConversation(@RequestBody ConversationRequest conversationRequest){
        conversationService.createConversation(conversationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/add-members")
    public ResponseEntity<Void> addMemberToConversation(@RequestBody AddMembersRequest request){
        conversationService.addMembersToConversation(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationContentResponse> getConversationContent(@PathVariable UUID id){
        List<MessageRecord> messages = conversationService.getConversationContent(id);
        return ResponseEntity.ok().body(ConversationContentResponse.fromListOfMessagesToResponse(messages));
    }

}