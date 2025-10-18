package com.app.prod.issues.strategy;

import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AreaIssue implements IssueNotifyingStrategy{

    private final Validate validate;

    @Override
    public void validateAbilityToReportIssue(UsersRecord user, IssueRequest request) {
        validate.area(request.areaId());
        validate.thatUserBelongsToArea(request.areaId(), user);
    }

    @Override
    public void notifyAboutIssue() {

    }
}
