package com.app.prod.polls.dto;

import java.util.UUID;

public record PollRequest(
        String title,
        String description,
        UUID areaId,
        UUID buildingId,
        boolean anonymous
) {
}
