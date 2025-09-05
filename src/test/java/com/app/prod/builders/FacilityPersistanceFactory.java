package com.app.prod.builders;

import com.app.prod.facilities.enums.FacilityType;
import com.app.prod.facilities.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.FacilitiesRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacilityPersistanceFactory {

    private final Clock clock;
    private final FacilityRepository facilityRepository;

    public Builder getNewFacility(){ return new Builder(); }

    public class Builder {

        private final FacilitiesRecord instance;

        public Builder() {
            instance = new FacilitiesRecord();
        }

        public Builder buildingId(UUID id){
            instance.setBuildingId(id);
            return this;
        }

        public Builder withRandomValues(){
            instance.setId(UUID.randomUUID());
            instance.setName("Pool");
            instance.setType(FacilityType.BASIC.name());
            instance.setCapacity(10);
            instance.setLocation("Basement");
            instance.setRequiresApproval(false);
            return this;
        }

        public FacilitiesRecord build() {
            return instance;
        }

        public FacilitiesRecord buildAndSave() {
            FacilitiesRecord record = build();
            facilityRepository.insertOne(record);
            return record;
        }
    }

}
