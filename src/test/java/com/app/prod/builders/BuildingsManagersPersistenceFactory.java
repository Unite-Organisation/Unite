package com.app.prod.builders;

import com.app.prod.building.repository.BuildingsManagersRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.BuildingManagerRecord;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildingsManagersPersistenceFactory {

    private final BuildingsManagersRepository buildingsManagersRepository;

    public Builder addNewBuildingManager(){ return new Builder(); }

    public class Builder {

        private final BuildingManagerRecord instance;

        public Builder() {
            instance = new BuildingManagerRecord();
        }

        public Builder buildingId(UUID buildingId) {
            instance.setBuildingId(buildingId);
            return this;
        }

        public Builder managerId(UUID userId) {
            instance.setUserId(userId);
            return this;
        }

        public BuildingManagerRecord build() {
            instance.setId(UUID.randomUUID());
            return instance;
        }

        public BuildingManagerRecord buildAndSave() {
            BuildingManagerRecord record = build();
            buildingsManagersRepository.insertOne(record);
            return record;
        }
    }
}
