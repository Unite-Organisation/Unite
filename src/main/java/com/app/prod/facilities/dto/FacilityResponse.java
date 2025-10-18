package com.app.prod.facilities.dto;

import com.app.prod.facilities.enums.FacilityType;

import java.util.UUID;

public record FacilityResponse(
        UUID id,
        String name,
        String type,
        Integer capacity,
        String location,
        Boolean requiresApproval
) {
}
