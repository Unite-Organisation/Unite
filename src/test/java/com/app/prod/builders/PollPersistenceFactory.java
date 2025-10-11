package com.app.prod.builders;

import com.app.prod.polls.repository.PollRepository;
import com.app.prod.utils.TestData;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.PollsRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PollPersistenceFactory {

    private final Clock clock;
    private final PollRepository pollRepository;

    public Builder getNewPoll() {
        return new Builder();
    }

    public class Builder {

        private final PollsRecord instance;

        public Builder() {
            instance = new PollsRecord();
        }

        public Builder id(UUID id) {
            instance.setId(id);
            return this;
        }

        public Builder title(String title) {
            instance.setTitle(title);
            return this;
        }

        public Builder description(String description) {
            instance.setDescription(description);
            return this;
        }

        public Builder areaId(UUID areaId) {
            instance.setAreaId(areaId);
            return this;
        }

        public Builder buildingId(UUID buildingId) {
            instance.setBuildingId(buildingId);
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            instance.setCreatedBy(createdBy);
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            instance.setStartTime(startTime);
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            instance.setEndTime(endTime);
            return this;
        }

        public Builder anonymous(boolean anonymous) {
            instance.setAnonymous(anonymous);
            return this;
        }

        public Builder finished(boolean finished) {
            instance.setFinished(finished);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            instance.setCreatedAt(createdAt);
            return this;
        }

        public Builder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setTitle(TestData.title());
            instance.setDescription(TestData.description());
            instance.setAnonymous(false);
            instance.setFinished(false);
            instance.setCreatedAt(LocalDateTime.now(clock));
            instance.setStartTime(LocalDateTime.now(clock));
            instance.setEndTime(LocalDateTime.now(clock).plusDays(3));
            return this;
        }

        public PollsRecord build() {
            return instance;
        }

        public PollsRecord buildAndSave() {
            PollsRecord record = build();
            pollRepository.insertOne(record);
            return record;
        }
    }
}