package com.app.prod.area.service;

import com.app.prod.area.dto.AreaCreateRequest;
import com.app.prod.area.mappers.AreaMapper;
import com.app.prod.area.repository.AreaRepository;
import com.app.prod.building.mappers.BuildingMapper;
import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.building.repository.BuildingsManagersRepository;
import com.app.prod.config.security.TokenSecurityManager;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.service.UserRoleService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.BuildingManagerRecord;
import org.jooq.sources.tables.records.BuildingRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AreaService {

    private final AreaRepository areaRepository;
    private final BuildingRepository buildingRepository;
    private final BuildingsManagersRepository buildingsManagersRepository;
    private final Clock clock;
    private final TokenSecurityManager tokenSecurityManager;
    private final UserRoleService userRoleService;

    @Transactional
    public String createArea(AreaCreateRequest request){
        LocalDateTime now = LocalDateTime.now(clock);
        UUID areaId = UUID.randomUUID();

        areaRepository.insertOne(AreaMapper.fromRequestToRecord(request, areaId, now));
        saveBuildings(request.buildings(), areaId);

        log.info("Created area: {}", request.name());
        return String.format("Area with id: %s has been created.", areaId);
    }

    private void saveBuildings(List<AreaCreateRequest.BuildingRequest> buildings, UUID areaId){
        var managerId = tokenSecurityManager.getCurrentUser().getId();
        List<BuildingRecord> buildingRecords = BuildingMapper.fromRequestToList(buildings, areaId);
        buildingRepository.insertMany(buildingRecords);

        buildingsManagersRepository.insertMany(
                buildingRecords.stream()
                        .map(record -> new BuildingManagerRecord(
                                UUID.randomUUID(),
                                record.getId(),
                                managerId
                        ))
                        .toList()
        );
    }

    public UUID getUserArea(AppUserRecord user){
        UserRole role = userRoleService.getUserRoleFromId(user.getUserRole());
        return switch (role){
            case RESIDENT -> getResidentArea(user);
            case MANAGER -> getManagersArea(user);
            case ADMIN -> null;
        };
    }

    private UUID getManagersArea(AppUserRecord user) {
        return areaRepository.getManagersArea(user.getId());
    }

    private UUID getResidentArea(AppUserRecord user) {
        return areaRepository.getResidentArea(user.getId());
    }

    public UUID getAreaManager(UUID areaId){
        return areaRepository.getAreaManager(areaId);
    }
}