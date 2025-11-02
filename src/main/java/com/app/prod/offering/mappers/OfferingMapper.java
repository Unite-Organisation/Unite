package com.app.prod.offering.mappers;

import com.app.prod.offering.dto.OfferingRequest;
import org.jooq.sources.tables.records.OfferingRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public class OfferingMapper {

    public static OfferingRecord fromRequestToRecord(OfferingRequest request, UUID userId, LocalDateTime now, UUID areaId){
        return new OfferingRecord(
                UUID.randomUUID(),
                request.title(),
                request.description(),
                request.category(),
                request.isActive(),
                areaId,
                userId,
                request.price(),
                request.endDate(),
                now
        );
    }

}
