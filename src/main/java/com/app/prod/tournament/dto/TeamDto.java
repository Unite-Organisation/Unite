package com.app.prod.tournament.dto;

import java.util.List;
import java.util.UUID;

public record TeamDto(
        UUID id,
        String name,
        List<ParticipantDto> members
) {
}
