package com.app.prod.tournament.models;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class Tournament {

    private final UUID id;
    private final String name;
    private final List<Match> matches;

    public Tournament(UUID id, String name, List<Match> matches) {
        this.id = id;
        this.name = name;
        this.matches = matches;
    }
}
