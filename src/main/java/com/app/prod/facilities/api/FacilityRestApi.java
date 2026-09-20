package com.app.prod.facilities.api;

import com.app.prod.access.BuildingScope;
import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.facilities.dto.*;
import com.app.prod.facilities.service.FacilityFilteringService;
import com.app.prod.facilities.service.FacilityService;
import com.app.prod.facilities.service.ReservationService;
import com.app.prod.utils.filters.FacilityFilter;
import com.app.prod.utils.filters.FacilityReservationFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/facility")
@RequiredArgsConstructor
@Tag(name = "Facilities")
public class FacilityRestApi {

    private final ReservationService reservationService;
    private final GlobalSecurityManager globalSecurityManager;
    private final FacilityService facilityService;
    private final FacilityFilteringService facilityFilteringService;

    @GetMapping()
    @PreAuthorize("hasAnyRole('MANAGER', 'RESIDENT', 'ADMIN')")
    public List<FacilityResponse> getFacilities(BuildingScope scope, @Valid @ModelAttribute FacilityFilterRequest request){
        FacilityFilter filter = facilityFilteringService.prepareFilter(scope, request);
        return facilityService.getFacilities(request.pagination(), filter);
    }

    @GetMapping("/{facilityId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'RESIDENT', 'ADMIN')")
    public List<FacilitySlot> getAvailability(@PathVariable UUID facilityId, @Valid @ModelAttribute FacilityAvailabilityFilterRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        FacilityReservationFilter filter = facilityFilteringService.prepareFilter(facilityId, userId, request);
        return reservationService.getFacilityAvailability(facilityId, request.view(), filter);
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasAnyRole('RESIDENT')")
    public ReservationResponse reserveFacility(@Valid @RequestBody ReservationRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        return reservationService.reserve(request, userId);
    }

}
