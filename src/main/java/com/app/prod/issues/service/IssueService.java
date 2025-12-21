package com.app.prod.issues.service;

import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.issues.dto.StrategyOptions;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.issues.repository.IssueRepository;
import com.app.prod.issues.strategy.IssueNotifyingStrategy;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.service.UserRoleService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class IssueService {

    private final IssueStrategyFactory factory;
    private final IssuePersistingService issuePersistingService;
    private final IssueRepository issueRepository;
    private final Validate validate;
    private final IssueStatusService issueStatusService;
    private final UserRoleService userRoleService;

    public void createIssue(IssueRequest request, AppUserRecord user) {
        IssueNotifyingStrategy issueStrategy = factory.chooseStrategy(new StrategyOptions(
                request.buildingId(),
                request.areaId(),
                request.facilityId(),
                request.pollId()
        ));

        issueStrategy.validateAbilityToReportIssue(user, request);
        var issue = issuePersistingService.saveIssue(request, user.getId());
        issueStrategy.notifyAboutIssue(issue);
    }

    public void updateIssueStatus(UUID issueId, IssueProcessingStatus updatedStatus, AppUserRecord user){
        var issue = validate.andGetIssue(issueId);
        var userRole = userRoleService.getUserRoleFromId(user.getUserRole());
        issueStatusService.checkIfStatusUpdateIsPossible(IssueProcessingStatus.valueOf(issue.getStatus()), updatedStatus, userRole);
        issueRepository.updateStatus(issueId, updatedStatus);
    }
}
