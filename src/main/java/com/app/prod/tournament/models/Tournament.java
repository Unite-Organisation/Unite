package com.app.prod.tournament.models;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class Tournament {

    private final UUID id;
    private final String name;
    private final List<Match> matches;
    private final TournamentStatus status;
    private final UUID creatorId;

    public Tournament(UUID id, String name, List<Match> matches, TournamentStatus status, UUID creatorId) {
        this.id = id;
        this.name = name;
        this.matches = matches;
        this.status = status;
        this.creatorId = creatorId;
    }
}
