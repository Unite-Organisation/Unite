package com.app.prod.user.dto;

import java.util.UUID;

public record PotentialContactResponse(
        BasicUserData basicUserData,
        UUID buildingId,
        String buildingName
) {
}
