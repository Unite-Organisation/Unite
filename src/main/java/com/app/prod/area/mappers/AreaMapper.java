package com.app.prod.area.mappers;

import com.app.prod.area.dto.AreaCreateRequest;
import org.jooq.sources.tables.records.AreasRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public class AreaMapper {

    public static AreasRecord fromRequestToRecord(AreaCreateRequest request, UUID id, LocalDateTime now){
        return new AreasRecord(
                id,
                request.name(),
                request.country(),
                request.city(),
                request.type().name(),
                now
        );
    }

}
