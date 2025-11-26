package com.app.prod.building.dto;

import com.app.prod.area.enums.AreaType;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;

import java.util.UUID;

public record HomePageResponse(
        UUID userId,
        String username,
        UserRole role,
        UserStatus status,

        UUID areaId,
        String areaName,
        AreaType areaType,
        UUID buildingId,
        String buildingName,
        String country,
        String street,
        String number
) {
}
