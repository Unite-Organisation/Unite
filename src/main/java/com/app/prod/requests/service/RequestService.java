package com.app.prod.requests.service;

import com.app.prod.building.service.BuildingService;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.requests.dto.RequestRequest;
import com.app.prod.requests.dto.RequestResponse;
import com.app.prod.requests.enums.RequestStatus;
import com.app.prod.requests.mappers.RequestMapper;
import com.app.prod.requests.repository.RequestRepository;
import com.app.prod.utils.filters.RequestFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.RequestRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final RequestRepository requestRepository;
    private final BuildingService buildingService;
    private final Clock clock;

    public void createRequest(RequestRequest request, AppUserRecord user){
        var now = LocalDateTime.now(clock);
        var areaId = buildingService.getAreaId(user.getBuildingId());

        requestRepository.insertOne(RequestMapper.fromRequestToRecord(request, areaId, user, now));
        log.info("Saved request from user {} at {}", user.getId(), now);
    }

    public RequestRecord getRequest(UUID requestId){
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.REQUEST_NOT_FOUND)));
    }

    public List<RequestResponse> getAllRequests(AppUserRecord user, RequestStatus status) {
        var areaId = buildingService.getAreaId(user.getBuildingId());
        var filter = prepareFilter(areaId, status, null);
        return requestRepository.findAllForArea(filter);
    }

    public List<RequestResponse> getMyRequests(AppUserRecord user) {
        var areaId = buildingService.getAreaId(user.getBuildingId());
        var filter = prepareFilter(areaId, null, user.getId());
        return requestRepository.findAllForArea(filter);
    }

    private RequestFilter prepareFilter(UUID areaId, RequestStatus status, UUID creator){
        return RequestFilter.builder()
                .areaId(Optional.ofNullable(areaId))
                .status(Optional.ofNullable(status))
                .requestCreatorId(Optional.ofNullable(creator))
                .build();
    }
}
