package com.app.prod.requests.service;

import com.app.prod.building.service.BuildingService;
import com.app.prod.requests.dto.RequestRequest;
import com.app.prod.requests.mappers.RequestMapper;
import com.app.prod.requests.repository.RequestDonorRepository;
import com.app.prod.requests.repository.RequestRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.AppUser;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.app.prod.requests.enums.RequestStatus.CANCELLED_BY_AUTHOR;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final RequestRepository requestRepository;
    private final RequestDonorRepository requestDonorRepository;
    private final BuildingService buildingService;
    private final Clock clock;
    private final Validate validate;

    public void createRequest(RequestRequest request, AppUserRecord user){
        var now = LocalDateTime.now(clock);
        var areaId = getUserAreaId(user);

        requestRepository.insertOne(RequestMapper.fromRequestToRecord(request, areaId, user, now));
        log.info("Saved request from user {} at {}", user.getId(), now);
    }

    private UUID getUserAreaId(AppUserRecord user){
        return buildingService.getAreaId(user.getBuildingId());
    }

    public void cancelRequest(UUID requestId, AppUserRecord user) {
        validate.thatThisRequestBelongsToUser(user, requestId);
        requestRepository.updateStatus(CANCELLED_BY_AUTHOR, requestId);
        log.info("Updated request {} status to {}", requestId, CANCELLED_BY_AUTHOR);
    }
}
