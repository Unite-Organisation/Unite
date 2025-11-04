package com.app.prod.offering.service;

import com.app.prod.building.repository.BuildingRepository;
import com.app.prod.building.service.BuildingService;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.offering.dto.OfferingRequest;
import com.app.prod.offering.dto.OfferingResponse;
import com.app.prod.offering.enums.OfferingCategory;
import com.app.prod.offering.mappers.OfferingMapper;
import com.app.prod.offering.repository.OfferingRepository;
import com.app.prod.utils.filters.OfferingFilter;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.OfferingRecord;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        offeringRepository.insertOne(OfferingMapper.fromRequestToRecord(request, usersRecord.getId(), now, areaId));
    }

    public List<OfferingResponse> getOfferings(OfferingFilter filter) {
        return offeringRepository.getOfferings(filter);
    }

    public void cancelOffering(UsersRecord user, UUID offeringId) {
        OfferingRecord offering = validate.offer(offeringId, user);

        if(!offering.getIsActive()){
            throw new BadRequestException("Offering is already finished or cancelled");
        }

        offeringRepository.cancelOffering(offeringId);
    }
}
