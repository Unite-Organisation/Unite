package com.app.prod.facilities.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.facilities.dto.FacilityAvailabilityFilterRequest;
import com.app.prod.facilities.dto.FacilityFilterRequest;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.FacilityFilter;
import com.app.prod.utils.filters.FacilityReservationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FacilityFilteringService {

    public FacilityFilter prepareFilter(BuildingScope scope, FacilityFilterRequest request) {
        return FacilityFilter.builder()
                .buildingId(scope.buildingId())
                .facilityType(Optional.ofNullable(request.getFacilityType()))
                .requiresApproval(Optional.ofNullable(request.getRequiresApproval()))
                .capacity(ComparisonFilter.of(request.getCapacity(), request.getCapacityModifier()))
                .build();
    }

    public FacilityReservationFilter prepareFilter(UUID facilityId, UUID viewerId, FacilityAvailabilityFilterRequest request) {
        validateNarrowings(request);

        return FacilityReservationFilter.builder()
                .facilityId(facilityId)
                .day(request.getDay())
                .userId(onlyOwnBookings(request) ? Optional.of(viewerId) : Optional.empty())
                .build();
    }

    private void validateNarrowings(FacilityAvailabilityFilterRequest request) {
        List<String> requested = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getAvailableOnly())) {
            requested.add("availableOnly");
        }
        if (Boolean.TRUE.equals(request.getTakenOnly())) {
            requested.add("takenOnly");
        }
        if (onlyOwnBookings(request)) {
            requested.add("myBookingsOnly");
        }

        if (requested.size() > 1) {
            throw new BadRequestException(AppError.of(
                    Code.CONFLICTING_FILTERS,
                    String.format("%s exclude each other, request at most one of them", String.join(" and ", requested))
            ));
        }
    }

    private boolean onlyOwnBookings(FacilityAvailabilityFilterRequest request) {
        return Boolean.TRUE.equals(request.getMyBookingsOnly());
    }

}
