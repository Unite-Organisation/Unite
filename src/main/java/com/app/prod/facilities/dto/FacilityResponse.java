package com.app.prod.facilities.dto;


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
