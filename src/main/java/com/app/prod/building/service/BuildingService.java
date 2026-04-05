package com.app.prod.building.service;

import com.app.prod.internal.clients.ChattingServiceClient;
import com.app.prod.internal.dtos.ConversationBulkActionDto;
import com.app.prod.job.async.AsyncJobRunner;
import com.app.prod.building.dto.BuildingResponse;
import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.building.repository.BuildingsManagersRepository;
import com.app.prod.conversation.service.ConversationService;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.job.jobs.ConversationsCreateJob;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.service.UserRoleService;
import com.app.prod.user.service.UserService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.Building;
import org.jooq.sources.tables.records.AppUserRecord;
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
    private final UserRoleService userRoleService;
    private final Validate validate;
    private final AsyncJobRunner asyncJobRunner;

    public String addUser(UUID userId, UUID buildingId, AppUserRecord manager) {
        validate.user(userId);
        validate.building(buildingId);
        validate.thatUserIsNotInAnyBuildingYet(userId);
        userService.addUsersBuilding(userId, buildingId);
        var areaId = getAreaId(buildingId);

        List<UUID> contacts = userService.getAllUsersInAreaWithoutUser(userId, areaId);
        UUID managerId = manager.getId();
        contacts.add(managerId);

        ConversationsCreateJob job = new ConversationsCreateJob(new ConversationBulkActionDto(userId, contacts));
        asyncJobRunner.execute(job);

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

    public List<BuildingResponse> getUsersBuildings(AppUserRecord user) {
        UserRole role = userRoleService.getUserRoleFromId(user.getUserRole());
        return switch (role){
            case RESIDENT -> buildingRepository.getResidentsBuilding(user.getBuildingId());
            case MANAGER -> buildingsManagersRepository.getManagersBuildings(user.getId());
            case ADMIN -> List.of();
        };
    }

    public UUID getAreaId(UUID buildingId){
        return findById(buildingId).getAreaId();
    }
}
