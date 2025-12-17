package com.app.prod.facilities.dto;

import java.util.List;

    public record BuildingFacilitiesResponse(
        List<FacilityResponse> facilityResponseList
    ) {
    }
