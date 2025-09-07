package com.app.prod.services.schedulers;

import com.app.prod.polls.repository.PollOptionRepository;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.polls.repository.PollResultRepository;
import com.app.prod.polls.repository.PollVotesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.PollsRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollScheduler {

    private final PollResultRepository pollResultRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVotesRepository pollVotesRepository;
    private final PollRepository pollRepository;
    private final Clock clock;

    @Scheduled(cron = "0 0 * * * *")
    public void finishPoll(){
        LocalDateTime now = LocalDateTime.now(clock);
        log.info("Started scheduler for finishing polls at: {}", now);

        var polls = pollRepository.getUnfinishedPollsAndFinishThem(now);
        for(var poll : polls){
            log.info("Finishing poll {}", poll.getTitle());
            handlePoll(poll);
        }

    }

    private void handlePoll(PollsRecord poll){
        var pollVotes = pollVotesRepository.getAllVotesForPoll(poll.getId());
        HashMap<UUID, Integer> result = new HashMap<>();

        pollVotes.stream()
                .forEach(vote -> {
                    result.merge(vote.getOptionId(), 1, Integer::sum);
                });
    }

}
