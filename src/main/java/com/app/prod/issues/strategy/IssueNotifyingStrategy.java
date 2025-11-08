package com.app.prod.issues.strategy;

import com.app.prod.issues.dto.IssueRequest;
import org.jooq.sources.tables.records.IssueRecord;
import org.jooq.sources.tables.records.AppUserRecord;

public interface IssueNotifyingStrategy {
    void validateAbilityToReportIssue(AppUserRecord user, IssueRequest request);
    void notifyAboutIssue(IssueRecord issue);
}
