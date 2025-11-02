package com.app.prod.offering.service;

import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.building.service.BuildingService;
import com.app.prod.offering.dto.OfferingRequest;
import com.app.prod.offering.mappers.OfferingMapper;
import com.app.prod.offering.repository.OfferingRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfferingService {

    private final OfferingRepository offeringRepository;
    private final BuildingService buildingService;
    private final Validate validate;
    private final Clock clock;

    public void createOffering(OfferingRequest request, UsersRecord usersRecord){
        validate.thatUserHasBuilding(usersRecord);
        var areaId = buildingService.getAreaId(usersRecord.getBuildingId());
        var now = LocalDateTime.now(clock);

        log.info("Saving offering from {} at {}", usersRecord.getId(), now);
        offeringRepository.insertOne(OfferingMapper.fromRequestToRecord(request, usersRecord.getId(), now, areaId);)
    }

}
