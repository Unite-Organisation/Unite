package com.app.prod.building.api;

import com.app.prod.building.dto.BuildingResponse;
import com.app.prod.building.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("buildings")
@RequiredArgsConstructor
public class BuildingRestApi {

    private final BuildingService buildingService;

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

}
