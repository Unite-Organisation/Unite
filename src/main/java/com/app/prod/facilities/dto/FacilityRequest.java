package com.app.prod.facilities.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record FacilityRequest(
        @NotEmpty
        List<Facility> facilities,
        @NotNull
        UUID buildingId
) {

    public record Facility(
            String name,
            String type,
            Integer capacity,
            String location,
            Boolean requiresApproval
    ){}

}


