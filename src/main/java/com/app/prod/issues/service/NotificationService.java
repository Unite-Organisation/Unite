package com.app.prod.issues.service;

import com.app.prod.building.repository.BuildingsManagersRepository;
import com.app.prod.facilities.repository.FacilityRepository;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.issues.repository.NotificationRepository;
import com.app.prod.polls.service.PollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.IssueRecord;
import org.jooq.sources.tables.records.NotificationRecord;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final BuildingsManagersRepository buildingsManagersRepository;
    private final FacilityRepository facilityRepository;
    private final PollService pollService;

    public void notifyManagerAboutBuildingIssue(IssueRecord issue){
        var managerId = fetchManagerIdForBuilding(issue.getBuildingId());
        persistNotification(issue, managerId);
    }

    public void notifyManagerAboutFacilityIssue(IssueRecord issue){
        var managerId = fetchManagerIdForFacility(issue.getFacilityId());
        persistNotification(issue, managerId);
    }

    public void notifyManagerAboutPollIssue(IssueRecord issue) {
        var managerId = fetchManagerIdForPoll(issue.getPollId());

        // in future - notify administrator

        persistNotification(issue, managerId);
    }


    private void persistNotification(IssueRecord issue, UUID managerId){
        var record = createNotification(issue, managerId);
        notificationRepository.insertOne(record);
    }

    private UUID fetchManagerIdForBuilding(UUID buildingId){
        return buildingsManagersRepository.fetchManagerIdForBuilding(buildingId);
    }

    private UUID fetchManagerIdForFacility(UUID facilityId){
        return facilityRepository.fetchManagerIdManagingFacility(facilityId);
    }

    private UUID fetchManagerIdForPoll(UUID pollId){
        var poll = pollService.findById(pollId);
        return poll.getCreatedBy();
    }

    private NotificationRecord createNotification(IssueRecord issue, UUID managerId){
        return new NotificationRecord(
                UUID.randomUUID(),
                issue.getId(),
                managerId,
                IssueProcessingStatus.SUBMITTED.name(),
                null
        );
    }

}
