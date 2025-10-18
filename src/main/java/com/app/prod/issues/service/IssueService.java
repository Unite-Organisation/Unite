package com.app.prod.issues.service;

import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.issues.dto.StrategyOptions;
import com.app.prod.issues.strategy.IssueNotifyingStrategy;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class IssueService {

    private final Validate validate;
    private final IssueStrategyFactory factory;

    public void createIssue(IssueRequest request, UsersRecord user) {
        IssueNotifyingStrategy issueStrategy = factory.chooseStrategy(new StrategyOptions(
                request.buildingId(),
                request.areaId(),
                request.facilityId(),
                request.pollId()
        ));

        issueStrategy.validateAbilityToReportIssue(user, request);
        issueStrategy.notifyAboutIssue();
    }
}
