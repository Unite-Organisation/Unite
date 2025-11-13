package com.app.prod.issues.strategy;

import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.IssueRecord;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AreaIssue implements IssueNotifyingStrategy{

    private final Validate validate;

    @Override
    public void validateAbilityToReportIssue(AppUserRecord user, IssueRequest request) {
        validate.area(request.areaId());
        validate.thatUserBelongsToArea(request.areaId(), user);
    }

    @Override
    public void notifyAboutIssue(IssueRecord issue) {
        // no need to notify anyone
        // to be implemented after we have area manager
    }
}
