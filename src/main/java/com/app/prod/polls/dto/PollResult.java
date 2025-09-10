package com.app.prod.polls.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PollResult (
        UUID winnerOption,
        List<PollOptionVoteCount> sortedVotes,
        List<PollOptionPercentageShare> sortedPercentageShareOfVotes,
        int numberOfVotes,
        int numberOfPeopleEligibleToVote,
        BigDecimal votersPercentage
){
}
