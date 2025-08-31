package com.app.prod.building.dto;

import java.util.UUID;

public record BuildingResponse(
        UUID id,
        String name,
        String country,
        String city,
        String street,
        String number,
        UUID areaId
) {
}
