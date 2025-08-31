package com.app.prod.building.mappers;

import com.app.prod.area.dto.AreaCreateRequest;
import org.jooq.sources.tables.records.BuildingsRecord;

import java.util.List;
import java.util.UUID;

public class BuildingMapper {

    public static BuildingsRecord fromRequestToRecord(AreaCreateRequest.BuildingRequest request, UUID areaId){
        var buildingId = UUID.randomUUID();

        return new BuildingsRecord(
                buildingId,
                request.name(),
                request.country(),
                request.city(),
                request.street(),
                request.number(),
                areaId
        );
    }

    public static List<BuildingsRecord> fromRequestToList(List<AreaCreateRequest.BuildingRequest> requests, UUID areaId){
        return requests.stream()
                .map(request -> fromRequestToRecord(request, areaId))
                .toList();
    }
}
