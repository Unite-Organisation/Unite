package com.app.prod.messaging.api;

import com.app.prod.messaging.dto.CreateMessageRequest;
import com.app.prod.messaging.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("message")
@RequiredArgsConstructor
@Deprecated
@Tag(name = "Messages")
public class MessageResiApi {

    private final MessageService messageService;

    @PostMapping()
    public ResponseEntity<String> createMessage(@RequestBody CreateMessageRequest request){
        String reponse = messageService.createMessage(request);
        return ResponseEntity.ok().body(reponse);
    }

}
