package com.app.prod.issues.repository;

import com.app.prod.issues.dto.IssueResponse;
import com.app.prod.issues.enums.IssuePriority;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.issues.repository.strategy.IssueJoiningStrategy;
import com.app.prod.user.enums.UserRole;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.sources.tables.Issue;
import org.jooq.sources.tables.records.IssueRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.*;

@Repository
public class IssueRepository extends BaseJooqRepository<Issue, IssueRecord, UUID> {
    protected IssueRepository(DSLContext dsl) {
        super(dsl, ISSUE, ISSUE.ID);
    }

    public List<IssueResponse> getEntityIssues(UUID entityId, IssueJoiningStrategy joiningStrategy) {
        var baseQuery = dslContext.select(
                ISSUE.ID,
                ISSUE.TITLE,
                ISSUE.DESCRIPTION,
                ISSUE.STATUS,
                ISSUE.PRIORITY,
                NOTIFICATION.SEEN_AT,
                USERS.FIRST_NAME,
                USERS.LAST_NAME,
                USER_ROLES.USER_ROLE
        )
                .from(ISSUE)
                .leftJoin(NOTIFICATION).on(NOTIFICATION.ISSUE_ID.eq(ISSUE.ID))
                .leftJoin(USERS).on(USERS.ID.eq(NOTIFICATION.RECIPIENT_ID))
                .leftJoin(USER_ROLES).on(USER_ROLES.ID.eq(USERS.USER_ROLE));

        var finalQuery = joiningStrategy.joinEntity(baseQuery, entityId);

        return finalQuery.orderBy(ISSUE.CREATED_AT)
                .fetch(record -> {
                        var recipientInfo = new IssueResponse.IssueRecipientInfo(
                                record.get(USERS.FIRST_NAME),
                                record.get(USERS.LAST_NAME),
                                UserRole.valueOf(record.get(USER_ROLES.USER_ROLE))
                        );

                        return new IssueResponse(
                                record.get(ISSUE.ID),
                                record.get(ISSUE.TITLE),
                                record.get(ISSUE.DESCRIPTION),
                                IssueProcessingStatus.valueOf(record.get(ISSUE.STATUS)),
                                IssuePriority.valueOf(record.get(ISSUE.PRIORITY)),
                                record.get(NOTIFICATION.SEEN_AT),
                                recipientInfo
                        );
                });

    }

    public void updateStatus(UUID issueId, IssueProcessingStatus updatedStatus) {
        dslContext.update(ISSUE)
                .set(ISSUE.STATUS, updatedStatus.name())
                .where(ISSUE.ID.eq(issueId))
                .execute();

    }
}
