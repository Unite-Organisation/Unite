package com.app.prod.tournament.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Team {
    private UUID id;
    private String name;
    private List<Participant> members;

    public Team(UUID id, String name, List<Participant> members) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("Team must have at least one member");
        }

        this.id = id;
        this.name = name;
        this.members = members;
    }
}
