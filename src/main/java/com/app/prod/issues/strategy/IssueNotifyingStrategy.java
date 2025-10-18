package com.app.prod.issues.strategy;

import com.app.prod.issues.dto.IssueRequest;
import org.jooq.sources.tables.records.UsersRecord;

public interface IssueNotifyingStrategy {
    void validateAbilityToReportIssue(UsersRecord user, IssueRequest request);
    void notifyAboutIssue();
}
