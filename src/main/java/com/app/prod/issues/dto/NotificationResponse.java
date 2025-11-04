package com.app.prod.issues.dto;

import com.app.prod.issues.enums.IssueObject;
import com.app.prod.issues.enums.IssuePriority;
import com.app.prod.issues.enums.IssueProcessingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID issueId,
        String issueTitle,
        String issueDescription,
        IssueProcessingStatus issueStatus,
        IssuePriority issuePriority,
        IssueObject issueObject,
        UUID entityId,
        IssuerData issuerData
) {
    public record IssuerData(
            UUID id,
            String firstName,
            String lastName,
            String userName,
            String userRole,
            LocalDateTime issueCreatedAt
    ){}
}
