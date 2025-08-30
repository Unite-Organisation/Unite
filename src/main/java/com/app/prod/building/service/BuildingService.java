package com.app.prod.building.service;

import com.app.prod.area.service.AreaService;
import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.service.UserService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.BuildingsRecord;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingService {

    private final UserService userService;
    private final BuildingRepository buildingRepository;
    private final Validate validate;

    public String addUser(UUID userId, UUID buildingId) {
        validate.user(userId);
        validate.building(buildingId);
        validate.thatUserIsNotInAnyBuildingYet(userId);
        var areaId = findById(buildingId).getAreaId();
        userService.addUsersBuilding(userId, buildingId, areaId);

        log.info("Added user {} to building {}", userId, buildingId);
        return String.format("Added user %s to building %s", userId, buildingId);
    }

    public BuildingsRecord findById(UUID buildingId){
        return buildingRepository.findById(buildingId).orElseThrow(
                () -> new EntityNotPresentException(String.format("Building with id: %s does not exist", buildingId))
        );
    }
}
