package com.app.prod.issues.strategy;

import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.issues.service.NotificationService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.IssueRecord;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PollIssue implements IssueNotifyingStrategy {

    private final Validate validate;
    private final NotificationService notificationService;

    @Override
    public void validateAbilityToReportIssue(AppUserRecord user, IssueRequest request) {
        validate.poll(request.pollId());
        validate.thatUserCanVote(user.getId(), request.pollId());
    }

    @Override
    public void notifyAboutIssue(IssueRecord issue) {
        notificationService.notifyManagerAboutPollIssue(issue);
    }
}
