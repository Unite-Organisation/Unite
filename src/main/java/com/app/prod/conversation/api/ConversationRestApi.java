package com.app.prod.conversation.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.conversation.dto.AddMembersRequest;
import com.app.prod.conversation.dto.ConversationContentResponse;
import com.app.prod.conversation.dto.ConversationRequest;
import com.app.prod.conversation.dto.ConversationResponse;
import com.app.prod.conversation.service.ConversationService;
import com.app.prod.utils.Pagination;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("conversation")
@RequiredArgsConstructor
@Tag(name = "Conversations")
public class ConversationRestApi {

    private final ConversationService conversationService;
    private final GlobalSecurityManager globalSecurityManager;

    @GetMapping()
    public ResponseEntity<List<ConversationResponse>> getConversations(@Valid @ModelAttribute Pagination pagination){
        var user = globalSecurityManager.getCurrentUser();
        List<ConversationResponse> conversations = conversationService.getConversations(user, pagination);
        return ResponseEntity.ok(conversations);
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
    public ResponseEntity<ConversationContentResponse> getConversationContent(
            @PathVariable UUID id,
            @Valid @ModelAttribute Pagination pagination
    ){
        ConversationContentResponse messages = conversationService.getConversationContent(id, pagination);
        return ResponseEntity.ok(messages);
    }

}