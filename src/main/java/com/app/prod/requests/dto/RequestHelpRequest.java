package com.app.prod.requests.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RequestHelpRequest(
        UUID requestId,
        LocalDateTime deadline
) {
}
