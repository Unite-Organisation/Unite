package com.app.prod.tournament.dto;

import java.util.UUID;

public record UpdateTournamentRequest(
        UUID matchId,
        UUID winnerTeamId
) {
}
