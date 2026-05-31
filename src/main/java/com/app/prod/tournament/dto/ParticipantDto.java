package com.app.prod.tournament.dto;

import java.util.UUID;

public record ParticipantDto(
        UUID id,
        String username
) {
}
