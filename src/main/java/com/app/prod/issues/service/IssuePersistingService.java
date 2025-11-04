package com.app.prod.issues.service;

import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.issues.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.IssueRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssuePersistingService {

    private final IssueRepository issueRepository;
    private final Clock clock;

    public IssueRecord saveIssue(IssueRequest request, UUID userId){
        IssueRecord record = new IssueRecord(
                UUID.randomUUID(),
                request.title(),
                request.description(),
                IssueProcessingStatus.SUBMITTED.name(),
                request.priority().name(),
                request.areaId(),
                request.buildingId(),
                request.facilityId(),
                request.pollId(),
                request.notifyEveryone(),
                userId,
                LocalDateTime.now(clock),
                request.issueObject().name()
        );

        issueRepository.insertOne(record);
        return record;
    }
}
