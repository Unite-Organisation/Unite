package com.app.prod.tournament.dto;

import java.util.List;
import java.util.UUID;

public record TournamentDto(
        UUID id,
        String name,
        List<RoundDto> rounds
) {
}
