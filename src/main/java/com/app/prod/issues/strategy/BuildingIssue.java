package com.app.prod.issues.strategy;

import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.issues.service.IssueService;
import com.app.prod.issues.service.NotificationService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.IssueRecord;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BuildingIssue implements IssueNotifyingStrategy{

    private final Validate validate;
    private final NotificationService notificationService;
    private final IssueService issueService;

    @Override
    public void validateAbilityToReportIssue(UsersRecord user, IssueRequest request) {
        validate.building(request.buildingId());
        validate.thatUserBelongsToBuilding(user, request.buildingId());
    }

    @Override
    public void notifyAboutIssue(IssueRecord issue) {
        notificationService.notifyManagerAboutBuildingIssue(issue);
    }
}
