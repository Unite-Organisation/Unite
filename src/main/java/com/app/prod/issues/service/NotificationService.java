package com.app.prod.issues.service;

import com.app.prod.building.repository.BuildingsManagersRepository;
import com.app.prod.facilities.repository.FacilityRepository;
import com.app.prod.issues.dto.NotificationResponse;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.issues.repository.IssueRepository;
import com.app.prod.issues.repository.NotificationRepository;
import com.app.prod.polls.service.PollService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.IssueRecord;
import org.jooq.sources.tables.records.NotificationRecord;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.app.prod.issues.enums.IssueProcessingStatus.SEEN_BY_RECIPIENT;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final IssueRepository issueRepository;
    private final BuildingsManagersRepository buildingsManagersRepository;
    private final FacilityRepository facilityRepository;
    private final PollService pollService;
    private final Clock clock;
    private final Validate validate;

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
                null
        );
    }

    public List<NotificationResponse> getNotifications(UUID managerId) {
        log.info("Fetching notifications for {}", managerId);
        return notificationRepository.getNotifications(managerId);
    }

    @Transactional
    public void updateNotificationSeenAtDate(UUID managerId, UUID notificationId) {
        validate.notification(notificationId);
        var now = LocalDateTime.now(clock);

        log.info("Manager {} has seen notification {} at {}", managerId, notificationId, now);
        var issueId = notificationRepository.updateSeenAtDateReturnIssueId(managerId, notificationId, now);

        log.info("Changing status of issue {} to {}", issueId, SEEN_BY_RECIPIENT.name());
        issueRepository.updateStatus(issueId, SEEN_BY_RECIPIENT);
    }
}
