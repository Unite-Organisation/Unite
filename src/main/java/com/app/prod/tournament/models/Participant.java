package com.app.prod.tournament.models;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Participant {
    private UUID id;
    private String username;

    public Participant(UUID id, String username) {
        this.id = id;
        this.username = username;
    }
}
