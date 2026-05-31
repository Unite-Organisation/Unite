package com.app.prod.tournament.dto;

import com.app.prod.tournament.models.MatchStatus;

import java.util.UUID;

public record MatchDto(
        UUID id,
        UUID nextMatchId,
        MatchStatus status,
        TeamDto teamA,
        TeamDto teamB,
        UUID winnerTeamId
) {
}
