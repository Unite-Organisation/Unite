package com.app.prod.building.service;

import com.app.prod.building.dto.BuildingResponse;
import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.building.repository.BuildingsManagersRepository;
import com.app.prod.config.security.TokenSecurityManager;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.user.service.UserService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.Building;
import org.jooq.sources.tables.records.BuildingRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingService {

    private final UserService userService;
    private final BuildingRepository buildingRepository;
    private final BuildingsManagersRepository buildingsManagersRepository;
    private final TokenSecurityManager tokenSecurityManager;
    private final Validate validate;

    public String addUser(UUID userId, UUID buildingId) {
        validate.user(userId);
        validate.building(buildingId);
        validate.thatUserIsNotInAnyBuildingYet(userId);
        userService.addUsersBuilding(userId, buildingId);

        log.info("Added user {} to building {}", userId, buildingId);
        return String.format("Added user %s to building %s", userId, buildingId);
    }

    public BuildingRecord findById(UUID buildingId){
        return buildingRepository.findById(buildingId).orElseThrow(
                () -> new EntityNotPresentException(
                        String.format("Building with id: %s does not exist", buildingId),
                        Building.class.getSimpleName()
                )
        );
    }

    public List<BuildingResponse> getManagerBuildings() {
        var user = tokenSecurityManager.getCurrentUser();
        return buildingsManagersRepository.getManagersBuildings(user.getId());
    }

    public UUID getAreaId(UUID buildingId){
        return findById(buildingId).getAreaId();
    }
}
