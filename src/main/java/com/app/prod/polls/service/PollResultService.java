package com.app.prod.polls.service;

import com.app.prod.polls.dto.PollOptionPercentageShare;
import com.app.prod.polls.dto.PollOptionVoteCount;
import com.app.prod.polls.dto.PollResult;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.polls.repository.PollVotesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.PollsRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollResultService {

    private final PollVotesRepository pollVotesRepository;
    private final PollRepository pollRepository;

    public PollResult calculatePollResult(UUID pollId) {
        List<PollOptionVoteCount> sortedVotes = pollVotesRepository.countVotes(pollId);
        int numberOfVotes = sortedVotes.stream().mapToInt(PollOptionVoteCount::count).sum();
        List<PollOptionPercentageShare> sortedOptionsPercentage = getPercentageList(sortedVotes, numberOfVotes);
        int numberOfPeopleEligibleToVote = getEligiblePeopleCount(pollId);

        log.info("Poll results = {} \n {} \n {} \n {}", sortedVotes, sortedOptionsPercentage, numberOfVotes, numberOfPeopleEligibleToVote);
        return createPollResultResponse(sortedVotes, sortedOptionsPercentage, numberOfVotes, numberOfPeopleEligibleToVote);
    }

    private int getEligiblePeopleCount(UUID pollId) {
        //TODO: optimization - two repository calls (validation and here)
        PollsRecord poll = pollRepository.findById(pollId).get();

        return switch (poll.getBuildingId()){
            case null -> pollRepository.getNumberOfPeopleEligibleToVoteAreaStrategy(pollId);
            default -> pollRepository.getNumberOfPeopleEligibleToVoteBuildingStrategy(pollId);
        };
    }


    private List<PollOptionPercentageShare> getPercentageList(List<PollOptionVoteCount> sortedVotes, int numberOfVotes){
        return sortedVotes.stream()
                .map(r -> new PollOptionPercentageShare(
                                r.optionId(),
                                BigDecimal.valueOf(r.count()).divide(BigDecimal.valueOf(numberOfVotes))
                        )
                ).toList();
    }

    private PollResult createPollResultResponse(
            List<PollOptionVoteCount> sortedVotes,
            List<PollOptionPercentageShare> sortedOptionsPercentage,
            int numberOfVotes,
            int numberOfPeopleEligibleToVote) {

        return new PollResult(
            sortedVotes.getFirst().optionId(),
            sortedVotes,
            sortedOptionsPercentage,
            numberOfVotes,
            numberOfPeopleEligibleToVote,
            BigDecimal.valueOf(numberOfVotes).divide(BigDecimal.valueOf(numberOfPeopleEligibleToVote))
        );
    }

}
