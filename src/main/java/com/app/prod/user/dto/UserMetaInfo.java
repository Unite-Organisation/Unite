package com.app.prod.user.dto;

import java.util.UUID;

public record UserMetaInfo(
        UUID userId,
        UUID buildingId,
        UUID areaId
) {
}
