package com.app.prod.area.dto;

import com.app.prod.area.enums.AreaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AreaInfoResponse(
        UUID id,
        String name,
        String country,
        String city,
        AreaType areaType,
        LocalDateTime createdAt,
        Integer usersNumber,
        List<BuildingInfoResponse> buildings
) {

    public record BuildingInfoResponse(
            UUID id,
            String name,
            String street,
            String number,
            Integer usersNumber
    ) {}
}
