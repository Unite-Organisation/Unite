package com.app.prod.facilities.service;

import com.app.prod.facilities.dto.FacilityRequest;
import com.app.prod.facilities.repository.FacilityRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.FacilitiesRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final Validate validate;

    public String addFacilities(FacilityRequest request) {
        var buildingId = request.buildingId();
        validate.building(buildingId);

        List<FacilitiesRecord> records = request.facilities().stream()
                .map(record -> new FacilitiesRecord(
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
}
