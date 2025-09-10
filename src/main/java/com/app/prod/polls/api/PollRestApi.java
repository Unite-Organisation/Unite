package com.app.prod.polls.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.dto.PollResponse;
import com.app.prod.polls.dto.PollResult;
import com.app.prod.polls.service.PollService;
import com.app.prod.utils.Pagination;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("poll")
@RequiredArgsConstructor
public class PollRestApi {

    private final GlobalSecurityManager globalSecurityManager;
    private final PollService pollService;

    @PostMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public void createPoll(@Valid @RequestBody PollRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        pollService.createPoll(request, userId);
    }

    @GetMapping()
    @PreAuthorize("hasRole('RESIDENT')")
    public List<PollResponse> getPolls(@Valid @ModelAttribute Pagination pagination){
        var userId = globalSecurityManager.getCurrentUser().getId();
        return pollService.getPolls(userId, pagination);
    }

    @PutMapping
    @PreAuthorize("hasRole('RESIDENT')")
    public void vote(
            @RequestParam UUID poll,
            @RequestParam UUID vote
    ){
        var userId = globalSecurityManager.getCurrentUser().getId();
        pollService.vote(userId, poll, vote);
    }

    @GetMapping("/result")
    public PollResult getPollResult(@RequestParam UUID pollId){
        return pollService.getPollResult(pollId);
    }

}
