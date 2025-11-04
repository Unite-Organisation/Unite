package com.app.prod.issues.dto;

import com.app.prod.issues.enums.IssueObject;
import com.app.prod.issues.enums.IssuePriority;

import java.util.UUID;

public record IssueRequest(
        String title,
        String description,
        IssuePriority priority,
        IssueObject issueObject,
        UUID areaId,
        UUID buildingId,
        UUID facilityId,
        UUID pollId,
        boolean notifyEveryone
) {}
