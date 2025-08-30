package com.app.prod.building.service;

import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.service.UserService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingService {

    private final UserService userService;
    private final Validate validate;

    public String addUser(UUID userId, UUID buildingId) {
        validate.user(userId);
        validate.building(buildingId);
        validate.thatUserIsNotInAnyBuildingYet(userId);
        userService.addBuilding(userId, buildingId);

        log.info("Added user {} to building {}", userId, buildingId);
        return String.format("Added user %s to building %s", userId, buildingId);
    }
}
