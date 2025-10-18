package com.app.prod.issues.dto;

import java.util.UUID;

public record IssueRequest(
        String title,
        String description,
        String priority,
        UUID areaId,
        UUID buildingId,
        UUID facilityId,
        UUID pollId,
        boolean notifyEveryone
) {}
