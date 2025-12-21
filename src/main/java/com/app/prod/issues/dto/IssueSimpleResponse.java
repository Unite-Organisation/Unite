package com.app.prod.issues.dto;

import com.app.prod.issues.enums.IssuePriority;
import com.app.prod.issues.enums.IssueProcessingStatus;

import java.util.UUID;

public record IssueSimpleResponse(
        UUID id,
        String title,
        String description,
        IssueProcessingStatus status,
        IssuePriority priority
) {
}
