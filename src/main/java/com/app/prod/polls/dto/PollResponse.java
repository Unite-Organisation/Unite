package com.app.prod.polls.dto;

import com.app.prod.polls.enums.PollTarget;
import com.app.prod.user.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PollResponse(
        UUID id,
        String title,
        String description,
        String author,
        String authorRole,
        boolean anonymous,
        PollTarget target,
        LocalDateTime pollStartTime,
        LocalDateTime pollEndTime,
        Boolean finished,
        Boolean userVoted,
        List<PollOptionResponse> options
) {
}
