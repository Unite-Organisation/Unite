package com.app.prod.conversation.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.conversation.dto.*;
import com.app.prod.conversation.service.ConversationService;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.shared.EntityCreatedResponse;
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
    public EntityCreatedResponse createConversation(@RequestBody ConversationRequest conversationRequest){
        return conversationService.createConversation(conversationRequest);
    }

    @PostMapping("/add-members")
    public void addMemberToConversation(@Valid @RequestBody AddMembersRequest request){
        var user = globalSecurityManager.getCurrentUser();
        conversationService.addMembersToConversation(request, user);
    }

    @GetMapping("/{id}")
    public List<ConversationMessageResponse> getConversationContent(
            @PathVariable UUID id,
            @Valid @ModelAttribute Pagination pagination
    ){
        return conversationService.getConversationContent(id, pagination);
    }

    @PostMapping("/group")
    public void createGroupConversation(@RequestBody GroupConversationRequest request){
        var user = globalSecurityManager.getCurrentUser();
        conversationService.createGroupConversation(request, user);
    }

}