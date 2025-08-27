package com.app.prod.area.service;

import com.app.prod.area.dto.AreaCreateRequest;
import com.app.prod.area.mappers.AreaMapper;
import com.app.prod.area.repository.AreaRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AreaService {

    private final AreaRepository areaRepository;
    private final Clock clock;
    private final Validate validate;

    public String createArea(AreaCreateRequest request){
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        areaRepository.insertOne(AreaMapper.fromRequestToRecord(request, id, now));

        log.info("Created area: {}", request.name());
        return String.format("Area with id: %s has been created.", id);
    }

    public String addUser(UUID areaId, UUID userId) {
        validate.area(areaId);
        validate.user(userId);

        //TODO: here logic need to be added

        return null;
    }
}
