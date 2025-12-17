package com.app.prod.builders;

import com.app.prod.facilities.enums.FacilityType;
import com.app.prod.facilities.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.FacilityRecord;
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

        private final FacilityRecord instance;

        public Builder() {
            instance = new FacilityRecord();
        }

        public Builder buildingId(UUID id){
            instance.setBuildingId(id);
            return this;
        }

        public Builder withRandomValues(){
            instance.setId(UUID.randomUUID());
            instance.setName("Pool");
            instance.setType(FacilityType.RECREATION.name());
            instance.setCapacity(10);
            instance.setLocation("Basement");
            instance.setRequiresApproval(false);
            return this;
        }

        public FacilityRecord build() {
            return instance;
        }

        public FacilityRecord buildAndSave() {
            FacilityRecord record = build();
            facilityRepository.insertOne(record);
            return record;
        }
    }

}
