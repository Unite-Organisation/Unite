package com.app.prod.polls.service;

import com.app.prod.polls.dto.PollOptionPercentageShare;
import com.app.prod.polls.dto.PollOptionVoteCount;
import com.app.prod.polls.dto.PollResult;
import com.app.prod.polls.repository.PollOptionRepository;
import com.app.prod.polls.repository.PollRepository;
import com.app.prod.polls.repository.PollVotesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.PollOptionRecord;
import org.jooq.sources.tables.records.PollRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollResultService {

    private final PollVotesRepository pollVotesRepository;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollProcessingService pollProcessingService;

    public PollResult calculatePollResult(UUID pollId) {
        List<PollOptionRecord> allPollOptions = pollOptionRepository.getAllOptionsForPoll(pollId);
        List<PollOptionVoteCount> sortedVotes = pollVotesRepository.countVotes(pollId);

        includeOptionsWithNoVotes(allPollOptions, sortedVotes);

        List<PollOptionVoteCount> winners = pollProcessingService.chooseWinners(pollId, sortedVotes);

        int numberOfVotes = pollProcessingService.getAllVotesCount(sortedVotes);
        List<PollOptionPercentageShare> sortedOptionsPercentage = getPercentageList(sortedVotes, numberOfVotes);
        int numberOfPeopleEligibleToVote = getEligiblePeopleCount(pollId);

        log.info("Poll results = {} \n {} \n {} \n {}",
                sortedVotes,
                sortedOptionsPercentage,
                numberOfVotes,
                numberOfPeopleEligibleToVote);

        return createPollResultResponse(
                winners,
                sortedVotes,
                sortedOptionsPercentage,
                numberOfVotes,
                numberOfPeopleEligibleToVote
        );
    }

    private void includeOptionsWithNoVotes(List<PollOptionRecord> allPollOptions, List<PollOptionVoteCount> sortedVotes){
        if(allPollOptions.size() == sortedVotes.size()){
            log.info("There are no poll options with zero votes");
            return;
        }

        for(var pollOption : allPollOptions){
            if(!pollOptionIsIncluded(pollOption, sortedVotes)){
                sortedVotes.add(new PollOptionVoteCount(
                        pollOption.getId(),
                        0, // - zero votes for this option
                        pollOption.getOptionText()
                ));
            }
        }
    }

    private boolean pollOptionIsIncluded(PollOptionRecord pollOption, List<PollOptionVoteCount> sortedVotes){
        for(var vote : sortedVotes){
            if(pollOption.getId().equals(vote.optionId())){
                return true;
            }
        }
        return false;
    }

    private int getEligiblePeopleCount(UUID pollId) {
        //TODO: optimization - two repository calls (validation and here)
        PollRecord poll = pollRepository.findById(pollId).get();

        return switch (poll.getBuildingId()){
            case null -> pollRepository.getNumberOfPeopleEligibleToVoteAreaStrategy(pollId);
            default -> pollRepository.getNumberOfPeopleEligibleToVoteBuildingStrategy(pollId);
        };
    }


    private List<PollOptionPercentageShare> getPercentageList(List<PollOptionVoteCount> sortedVotes, int numberOfVotes){
        return numberOfVotes != 0 ?
                getVotesPercentageList(sortedVotes, numberOfVotes) :
                zeroVotesPercentageList(sortedVotes);
    }

    private List<PollOptionPercentageShare> getVotesPercentageList(List<PollOptionVoteCount> sortedVotes, int numberOfVotes) {
        return sortedVotes.stream()
                .map(r -> new PollOptionPercentageShare(
                                r.optionId(),
                                BigDecimal.valueOf(r.count()).divide(BigDecimal.valueOf(numberOfVotes), 2, RoundingMode.HALF_UP)
                        )
                ).toList();
    }

    private List<PollOptionPercentageShare> zeroVotesPercentageList(List<PollOptionVoteCount> sortedVotes) {
        return sortedVotes.stream()
                .map(r -> new PollOptionPercentageShare(
                                r.optionId(),
                                BigDecimal.ZERO
                        )
                ).toList();
    }

    private PollResult createPollResultResponse(
            List<PollOptionVoteCount> winners,
            List<PollOptionVoteCount> sortedVotes,
            List<PollOptionPercentageShare> sortedOptionsPercentage,
            int numberOfVotes,
            int numberOfPeopleEligibleToVote) {

        return new PollResult(
            winners,
            sortedVotes,
            sortedOptionsPercentage,
            numberOfVotes,
            numberOfPeopleEligibleToVote,
            BigDecimal.valueOf(numberOfVotes).divide(BigDecimal.valueOf(numberOfPeopleEligibleToVote), 2, RoundingMode.HALF_UP)
        );
    }

}
