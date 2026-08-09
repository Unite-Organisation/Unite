package com.app.prod.building.api;

import com.app.prod.building.dto.BuildingResponse;
import com.app.prod.building.service.BuildingService;
import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.facilities.dto.FacilityRequest;
import com.app.prod.facilities.service.FacilityService;
import com.app.prod.utils.SimpleResponse;
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

    @PutMapping("/user")
    public ResponseEntity<SimpleResponse> addUserToBuilding(@RequestParam UUID buildingId, @RequestParam UUID userId){
        var manager = globalSecurityManager.getCurrentUser();
        String message = buildingService.addUser(userId, buildingId, manager);
        return ResponseEntity.ok(new SimpleResponse(message));
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('RESIDENT', 'MANAGER')")
    public ResponseEntity<List<BuildingResponse>> getUsersBuildings(){
        var user = globalSecurityManager.getCurrentUser();
        List<BuildingResponse> buildings = buildingService.getUsersBuildings(user);
        return ResponseEntity.ok(buildings);
    }

    @PostMapping("/facilities")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SimpleResponse> addFacilities(@Valid @RequestBody FacilityRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        String message = facilityService.addFacilities(request, userId);
        return ResponseEntity.ok(new SimpleResponse(message));
    }

}
