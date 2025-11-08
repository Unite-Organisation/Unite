package com.app.prod.building.mappers;

import com.app.prod.area.dto.AreaCreateRequest;
import org.jooq.sources.tables.records.BuildingRecord;

import java.util.List;
import java.util.UUID;

public class BuildingMapper {

    public static BuildingRecord fromRequestToRecord(AreaCreateRequest.BuildingRequest request, UUID areaId){
        var buildingId = UUID.randomUUID();

        return new BuildingRecord(
                buildingId,
                request.name(),
                request.country(),
                request.city(),
                request.street(),
                request.number(),
                areaId
        );
    }

    public static List<BuildingRecord> fromRequestToList(List<AreaCreateRequest.BuildingRequest> requests, UUID areaId){
        return requests.stream()
                .map(request -> fromRequestToRecord(request, areaId))
                .toList();
    }
}
