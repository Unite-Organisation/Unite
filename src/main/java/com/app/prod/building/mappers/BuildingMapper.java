package com.app.prod.building.mappers;

import com.app.prod.area.dto.AreaCreateRequest;
import org.jooq.sources.tables.records.BuildingRecord;

import java.util.List;
import java.util.UUID;

public class BuildingMapper {

    public static BuildingRecord fromRequestToRecord(AreaCreateRequest.BuildingRequest request, String country, String city, UUID areaId){
        var buildingId = UUID.randomUUID();

        return new BuildingRecord(
                buildingId,
                request.name(),
                country,
                city,
                request.street(),
                request.number(),
                areaId
        );
    }

    public static List<BuildingRecord> fromRequestToList(AreaCreateRequest requests, UUID areaId){
        return requests.buildings().stream()
                .map(request -> fromRequestToRecord(request, requests.country(), requests.city(), areaId))
                .toList();
    }
}
