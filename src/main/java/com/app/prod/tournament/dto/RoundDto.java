package com.app.prod.tournament.dto;

import java.util.List;

public record RoundDto(
        int roundNumber,
        String roundName,
        List<MatchDto> matches
) {
}
