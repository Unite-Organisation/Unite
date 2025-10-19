package com.app.prod.issues.dto;

import com.app.prod.issues.enums.IssuePriority;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.user.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record IssueResponse(
        UUID id,
        String title,
        String description,
        IssueProcessingStatus status,
        IssuePriority priority,
        LocalDateTime seenByRecipientAt,
        IssueRecipientInfo recipient
) {
    public record IssueRecipientInfo(
       String firstName,
       String lastName,
       UserRole role
    ) {}
}

