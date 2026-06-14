package com.app.prod.tournament.models;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Match {

    private final UUID id;
    private final UUID tournamentId;
    private final UUID nextMatchId;
    private final int roundNumber;

    private Team teamA;
    private Team teamB;
    private UUID winnerTeamId;
    private boolean isSkip;
    private MatchStatus matchStatus;

    public Match(UUID id, UUID tournamentId, UUID nextMatchId, int roundNumber) {
        this.id = id;
        this.tournamentId = tournamentId;
        this.nextMatchId = nextMatchId;
        this.roundNumber = roundNumber;
    }

}
