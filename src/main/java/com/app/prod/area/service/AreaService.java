package com.app.prod.area.service;

import com.app.prod.area.dto.AreaCreateRequest;
import com.app.prod.area.mappers.AreaMapper;
import com.app.prod.area.repository.AreaRepository;
import com.app.prod.area.repository.AreasUsersRepository;
import com.app.prod.building.mappers.BuildingMapper;
import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AreasUsersRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AreaService {

    private final AreaRepository areaRepository;
    private final BuildingRepository buildingRepository;
    private final AreasUsersRepository areasUsersRepository;
    private final Clock clock;
    private final Validate validate;

    public String createArea(AreaCreateRequest request){
        LocalDateTime now = LocalDateTime.now(clock);
        UUID areaId = UUID.randomUUID();
        areaRepository.insertOne(AreaMapper.fromRequestToRecord(request, areaId, now));
        buildingRepository.insertMany(BuildingMapper.fromRequestToList(request.buildings(), areaId));

        log.info("Created area: {}", request.name());
        return String.format("Area with id: %s has been created.", areaId);
    }

    public String addUser(UUID areaId, UUID userId) {
        validate.area(areaId);
        validate.user(userId);

        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();

        areasUsersRepository.insertOne(new AreasUsersRecord(
                id,
                areaId,
                userId,
                now
        ));

        log.info("Added user {} to area {}", userId, areaId);
        return String.format("Added use %s to area %s", userId, areaId);
    }
}
