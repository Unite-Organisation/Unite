package com.app.prod.polls.dto;

import java.util.UUID;

public record PollOptionResponse(
        UUID id,
        String content
) {
}
