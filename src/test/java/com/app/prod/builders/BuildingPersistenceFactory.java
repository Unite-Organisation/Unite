package com.app.prod.builders;

import com.app.prod.building.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.BuildingRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildingPersistenceFactory {

    private final Clock clock;
    private final BuildingRepository buildingRepository;

    public Builder getNewBuilding() {
        return new Builder();
    }

    public class Builder {

        private final BuildingRecord instance;

        public Builder() {
            instance = new BuildingRecord();
        }

        public Builder id(UUID id) {
            instance.setId(id);
            return this;
        }

        public Builder name(String name) {
            instance.setName(name);
            return this;
        }

        public Builder country(String country) {
            instance.setCountry(country);
            return this;
        }

        public Builder city(String city) {
            instance.setCity(city);
            return this;
        }

        public Builder street(String street) {
            instance.setStreet(street);
            return this;
        }

        public Builder number(String number) {
            instance.setNumber(number);
            return this;
        }

        public Builder areaId(UUID areaId) {
            instance.setAreaId(areaId);
            return this;
        }

        public Builder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setName("Building-" + UUID.randomUUID().toString().substring(0, 5));
            instance.setCountry("Poland");
            instance.setCity("Krakow");
            instance.setStreet("Main Street");
            instance.setNumber(String.valueOf((int) (Math.random() * 100)));
            return this;
        }

        public BuildingRecord build() {
            return instance;
        }

        public BuildingRecord buildAndSave() {
            BuildingRecord record = build();
            buildingRepository.insertOne(record);
            return record;
        }
    }
}
