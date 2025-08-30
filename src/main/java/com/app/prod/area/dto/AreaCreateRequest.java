package com.app.prod.area.dto;

import com.app.prod.area.enums.AreaType;

import java.util.List;

public record AreaCreateRequest(
        String name,
        String country,
        String city,
        AreaType type,
        List<BuildingRequest> buildings
) {

    public record BuildingRequest(
            String name,
            String country,
            String city,
            String street,
            String number
    ){}
}