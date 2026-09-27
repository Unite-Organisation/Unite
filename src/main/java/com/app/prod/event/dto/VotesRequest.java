package com.app.prod.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VotesRequest(
        @NotNull @Valid List<SlotVoteRequest> votes
) {
}
