package com.app.prod.building.api;

import com.app.prod.building.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
