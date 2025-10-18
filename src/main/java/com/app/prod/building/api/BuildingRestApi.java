package com.app.prod.building.api;

import com.app.prod.building.dto.BuildingResponse;
import com.app.prod.building.service.BuildingService;
import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.facilities.dto.BuildingFacilitiesResponse;
import com.app.prod.facilities.dto.FacilityRequest;
import com.app.prod.facilities.service.FacilityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("building")
@RequiredArgsConstructor
@Tag(name = "Buildings")
public class BuildingRestApi {

    private final BuildingService buildingService;
    private final FacilityService facilityService;
    private final GlobalSecurityManager globalSecurityManager;

    @PutMapping("/{buildingId}/user/{userId}")
    public ResponseEntity<String> addUserToBuilding(
            @PathVariable UUID buildingId,
            @PathVariable UUID userId){

        String message = buildingService.addUser(userId, buildingId);
        return ResponseEntity.ok(message);
    }

    @GetMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<BuildingResponse>> getManagerBuildings(){
        List<BuildingResponse> buildings = buildingService.getManagerBuildings();
        return ResponseEntity.ok(buildings);
    }

    @PostMapping("/facilities")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> addFacilities(@Valid @RequestBody FacilityRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        String message = facilityService.addFacilities(request, userId);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/{buildingId}/facilities")
    public BuildingFacilitiesResponse getFacilities(@PathVariable UUID buildingId){
        var user = globalSecurityManager.getCurrentUser();
        return facilityService.getFacilitiesForBuilding(buildingId, user);
    }

}
