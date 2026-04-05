package com.app.prod.schedulers;

import com.app.prod.polls.repository.PollRepository;
import com.app.prod.polls.service.PollProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollScheduler {

    private final PollRepository pollRepository;
    private final PollProcessingService pollProcessingService;
    private final Clock clock;

    // currently disabled on prod due to savings
    // @Scheduled(cron = "0 0 * * * *")
    public void finishPoll(){
        LocalDateTime now = LocalDateTime.now(clock);
        log.info("Started scheduler for finishing polls at: {}", now);

        var polls = pollRepository.getUnfinishedPollsAndFinishThem(now);

        if(polls.isEmpty()){
            log.info("No polls to finish, finishing job.");
            return;
        }

        for(var poll : polls){
            log.info("Finishing poll {}", poll.getTitle());
            pollProcessingService.finalizePoll(poll.getId());
        }

    }

}
