package com.app.prod.polls.service;

import com.app.prod.polls.dto.PollOptionVoteCount;
import com.app.prod.polls.repository.PollResultRepository;
import com.app.prod.polls.repository.PollVotesRepository;
import com.app.prod.polls.repository.PollWinnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.PollResultRecord;
import org.jooq.sources.tables.records.PollWinnerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PollProcessingService {

    private final PollVotesRepository pollVotesRepository;
    private final PollResultRepository pollResultRepository;
    private final PollWinnerRepository pollWinnerRepository;

    @Transactional
    public void finalizePoll(UUID pollId){
        List<PollOptionVoteCount> countedVotes = getCountedVotes(pollId);
        List<PollOptionVoteCount> winners = chooseWinners(pollId, countedVotes);
        int allVotesCount = getAllVotesCount(countedVotes);
        persistWinners(pollId, allVotesCount, winners);
    }

    public List<PollOptionVoteCount> chooseWinners(UUID pollId, List<PollOptionVoteCount> countedVotes){

        if(countedVotes.isEmpty()){
            log.info("No votes found for poll {}", pollId);
            return List.of();
        }

        return listOfWinners(countedVotes);
    }

    public List<PollOptionVoteCount> getCountedVotes(UUID pollId){
        return pollVotesRepository.countVotes(pollId);
    }

    public int getAllVotesCount(List<PollOptionVoteCount> countedVotes) {
        return countedVotes.stream()
                .mapToInt(PollOptionVoteCount::count)
                .sum();
    }

    private List<PollOptionVoteCount> listOfWinners(List<PollOptionVoteCount> countedVotes){
        List<PollOptionVoteCount> winners = new ArrayList<>();
        var winnerOption = countedVotes.getFirst();

        for(var vote : countedVotes){
            if(Objects.equals(vote.count(), winnerOption.count()) && vote.count() != 0){
                winners.add(vote);
            }
        }

        return winners;
    }

    @Transactional
    public void persistWinners(UUID pollId, int allVotes, List<PollOptionVoteCount> winners){
        List<PollWinnerRecord> winnersRecords = new ArrayList<>();
        for(var winner : winners){
            winnersRecords.add(new PollWinnerRecord(
                    UUID.randomUUID(),
                    pollId,
                    winner.optionId()
            ));
        }

        pollResultRepository.insertOne(new PollResultRecord(
                UUID.randomUUID(),
                pollId,
                allVotes,
                true
        ));

        pollWinnerRepository.insertMany(winnersRecords);
    }

}
