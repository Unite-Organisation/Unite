package com.app.prod.polls.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.polls.dto.PollRequest;
import com.app.prod.polls.dto.PollResponse;
import com.app.prod.polls.dto.PollResult;
import com.app.prod.polls.enums.PollStatus;
import com.app.prod.polls.service.PollFilteringService;
import com.app.prod.polls.service.PollService;
import com.app.prod.post.enums.PostType;
import com.app.prod.services.schedulers.PollScheduler;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.PollFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("poll")
@RequiredArgsConstructor
@Tag(name = "Polls")
public class PollRestApi {

    private final GlobalSecurityManager globalSecurityManager;
    private final PollService pollService;
    private final PollScheduler scheduler;
    private final PollFilteringService pollFilteringService;

    @PostMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public void createPoll(@Valid @RequestBody PollRequest request){
        var user = globalSecurityManager.getCurrentUser();
        pollService.createPoll(request, user);
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('RESIDENT', 'MANAGER')")
    public List<PollResponse> getPolls(
            @Valid @ModelAttribute Pagination pagination,
            @RequestParam(required = false) PollStatus pollStatus
    ){
        var userId = globalSecurityManager.getCurrentUser().getId();
        PollFilter pollFilter = pollFilteringService.prepareFilter(pollStatus);
        return pollService.getPolls(userId, pagination, pollFilter);
    }

    @PutMapping("/vote")
    @PreAuthorize("hasRole('RESIDENT')")
    public void vote(@RequestParam UUID poll, @RequestParam UUID vote){
        var user = globalSecurityManager.getCurrentUser();
        pollService.vote(user, poll, vote);
    }

    @GetMapping("/result")
    public PollResult getPollResult(@RequestParam UUID pollId){
        return pollService.getPollResult(pollId);
    }

    @PutMapping("/finish-all")
    public void internalFinishPoll(){
        scheduler.finishPoll();
    }

}
