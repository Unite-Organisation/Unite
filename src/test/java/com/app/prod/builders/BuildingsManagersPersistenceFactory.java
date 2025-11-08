package com.app.prod.builders;

import com.app.prod.building.repository.BuildingsManagersRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.BuildingsManagersRecord;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildingsManagersPersistenceFactory {

    private final BuildingsManagersRepository buildingsManagersRepository;

    public Builder addNewBuildingManager(){ return new Builder(); }

    public class Builder {

        private final BuildingsManagersRecord instance;

        public Builder() {
            instance = new BuildingsManagersRecord();
        }

        public Builder buildingId(UUID buildingId) {
            instance.setBuildingId(buildingId);
            return this;
        }

        public Builder managerId(UUID userId) {
            instance.setUserId(userId);
            return this;
        }

        public BuildingsManagersRecord build() {
            instance.setId(UUID.randomUUID());
            return instance;
        }

        public BuildingsManagersRecord buildAndSave() {
            BuildingsManagersRecord record = build();
            buildingsManagersRepository.insertOne(record);
            return record;
        }
    }
}
