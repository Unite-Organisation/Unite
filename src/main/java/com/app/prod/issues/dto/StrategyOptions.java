package com.app.prod.issues.dto;

import java.util.UUID;

public record StrategyOptions(
        UUID buildingId,
        UUID areaId,
        UUID facilityId,
        UUID pollId
) {
}
