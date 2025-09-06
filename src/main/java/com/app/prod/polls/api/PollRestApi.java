package com.app.prod.polls.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.service.PollService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("poll")
@RequiredArgsConstructor
public class PollRestApi {

    private final GlobalSecurityManager globalSecurityManager;
    private PollService pollService;

    @PostMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public void createPoll(@RequestBody PollRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        pollService.createPoll(request, userId);
    }

}
