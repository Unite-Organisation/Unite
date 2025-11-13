package com.app.prod.requests.dto;

import java.util.UUID;

public record RequestHelpResponse(
        UUID conversationId
) {
}
