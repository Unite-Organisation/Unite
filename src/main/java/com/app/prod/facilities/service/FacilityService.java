package com.app.prod.facilities.service;

import com.app.prod.building.repository.BuildingsManagersRepository;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.facilities.dto.BuildingFacilitiesResponse;
import com.app.prod.facilities.dto.FacilityRequest;
import com.app.prod.facilities.repository.FacilityRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.FacilityRecord;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final Validate validate;
    private final BuildingsManagersRepository buildingsManagersRepository;

    public String addFacilities(FacilityRequest request, UUID userId) {
        var buildingId = request.buildingId();
        validate.building(buildingId);

        if(!buildingsManagersRepository.managerManagesBuilding(buildingId, userId)){
            throw new BadRequestException(AppError.of(Code.MANAGER_NO_ACCESS));
        }

        List<FacilityRecord> records = request.facilities().stream()
                .map(record -> new FacilityRecord(
                        UUID.randomUUID(),
                        record.name(),
                        buildingId,
                        record.type(),
                        record.capacity(),
                        record.location(),
                        record.requiresApproval()
                ))
                .toList();

        facilityRepository.insertMany(records);
        return String.format("Added %s facilities.", records.size());
    }

    public BuildingFacilitiesResponse getFacilities(AppUserRecord user) {
        return new BuildingFacilitiesResponse(facilityRepository.getFacilities(user.getId()));
    }
}
