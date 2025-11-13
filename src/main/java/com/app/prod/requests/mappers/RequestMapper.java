package com.app.prod.requests.mappers;

import com.app.prod.requests.dto.RequestRequest;
import com.app.prod.requests.enums.RequestStatus;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.RequestRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public class RequestMapper {

    public static RequestRecord fromRequestToRecord(RequestRequest request, UUID areaId, AppUserRecord user, LocalDateTime now){
        return new RequestRecord(
                UUID.randomUUID(),
                request.title(),
                request.description(),
                true,
                areaId,
                user.getId(),
                RequestStatus.CREATED.name(),
                now
        );
    }
}
