package com.app.prod.builders;

import com.app.prod.issues.enums.IssueObject;
import com.app.prod.issues.enums.IssuePriority;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.issues.repository.IssueRepository;
import com.app.prod.utils.TestData;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.IssueRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssuePersistenceFactory {

    private final Clock clock;
    private final IssueRepository issueRepository;

    public Builder getNewIssue(IssueObject issueObject) {
        return new Builder(issueObject);
    }

    public class Builder {

        private final IssueRecord instance;

        public Builder(IssueObject issueObject) {
            instance = new IssueRecord();
            instance.setIssueObject(issueObject.name());
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

        public Builder status(IssueProcessingStatus status) {
            instance.setStatus(status.name());
            return this;
        }

        public Builder priority(IssuePriority priority) {
            instance.setPriority(priority.name());
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

        public Builder facilityId(UUID facilityId) {
            instance.setFacilityId(facilityId);
            return this;
        }

        public Builder pollId(UUID pollId) {
            instance.setPollId(pollId);
            return this;
        }

        public Builder notifyEveryone(boolean notifyEveryone) {
            instance.setNotifyEveryone(notifyEveryone);
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            instance.setCreatedBy(createdBy);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            instance.setCreatedAt(createdAt);
            return this;
        }

        public Builder issueObject(String issueObject) {
            instance.setIssueObject(issueObject);
            return this;
        }

        public Builder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setTitle(TestData.title());
            instance.setDescription(TestData.description());
            instance.setStatus(IssueProcessingStatus.SUBMITTED.name());
            instance.setPriority(IssuePriority.MEDIUM.name());
            instance.setAreaId(UUID.randomUUID());
            instance.setBuildingId(UUID.randomUUID());
            instance.setFacilityId(UUID.randomUUID());
            instance.setPollId(UUID.randomUUID());
            instance.setNotifyEveryone(false);
            instance.setCreatedBy(UUID.randomUUID());
            instance.setCreatedAt(LocalDateTime.now(clock));
            return this;
        }

        public IssueRecord build() {
            return instance;
        }

        public IssueRecord buildAndSave() {
            IssueRecord record = build();
            issueRepository.insertOne(record);
            return record;
        }
    }

    public void batchBuildAndSave(List<IssueRecord> issues) {
        issueRepository.insertMany(issues);
    }

}
