package com.app.prod.conversation.api;

import com.app.prod.conversation.dto.AddMembersRequest;
import com.app.prod.conversation.dto.ConversationRequest;
import com.app.prod.conversation.dto.ConversationResponse;
import com.app.prod.conversation.mappers.ConversationMapper;
import com.app.prod.conversation.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.ConversationsRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("conversation")
@RequiredArgsConstructor
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
    public ResponseEntity<?> addMemberToConversation(@RequestBody AddMembersRequest request){
        conversationService.addMembersToConversation(request);
        return ResponseEntity.ok().build();
    }

}