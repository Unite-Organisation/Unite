package com.app.prod.tournament.dto;

import com.app.prod.tournament.models.TournamentStatus;
import com.app.prod.tournament.models.TournamentType;

import java.util.List;
import java.util.UUID;

public record TournamentResponse(
        UUID id,
        String authorDisplayName,
        String name,
        String description,
        Integer teamSize,
        List<ParticipantResponse> participants,
        TournamentStatus status,
        TournamentType type,
        Boolean creator
) {
}
