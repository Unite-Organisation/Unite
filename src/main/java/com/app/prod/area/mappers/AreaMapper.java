package com.app.prod.area.mappers;

import com.app.prod.area.dto.AreaCreateRequest;
import org.jooq.sources.tables.records.AreaRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public class AreaMapper {

    public static AreaRecord fromRequestToRecord(AreaCreateRequest request, UUID id, LocalDateTime now){
        return new AreaRecord(
                id,
                request.name(),
                request.country(),
                request.city(),
                request.type().name(),
                now
        );
    }

}
