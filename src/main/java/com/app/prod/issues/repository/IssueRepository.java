package com.app.prod.issues.repository;

import com.app.prod.issues.dto.IssueResponse;
import com.app.prod.issues.dto.IssueSimpleResponse;
import com.app.prod.issues.enums.IssuePriority;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.issues.repository.strategy.IssueJoiningStrategy;
import com.app.prod.user.enums.UserRole;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.sources.tables.AppUser;
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
        AppUser ISSUER_USER = APP_USER.as("issuer_user");
        AppUser RECIPIENT_USER = APP_USER.as("recipient_user");
        org.jooq.sources.tables.UserRole RECIPIENT_ROLE = USER_ROLE.as("recipient_role");

        var baseQuery = dslContext.select(
                        ISSUE.ID,
                        ISSUE.TITLE,
                        ISSUE.DESCRIPTION,
                        ISSUE.STATUS,
                        ISSUE.PRIORITY,
                        NOTIFICATION.SEEN_AT,
                        RECIPIENT_USER.FIRST_NAME.as("recipient_fn"),
                        RECIPIENT_USER.LAST_NAME.as("recipient_ln"),
                        RECIPIENT_ROLE.USER_ROLE_.as("recipient_role_val"),
                        ISSUER_USER.FIRST_NAME.as("issuer_fn"),
                        ISSUER_USER.LAST_NAME.as("issuer_ln")
                )
                .from(ISSUE)
                .leftJoin(NOTIFICATION).on(NOTIFICATION.ISSUE_ID.eq(ISSUE.ID))
                .leftJoin(RECIPIENT_USER).on(RECIPIENT_USER.ID.eq(NOTIFICATION.RECIPIENT_ID))
                .leftJoin(RECIPIENT_ROLE).on(RECIPIENT_ROLE.ID.eq(RECIPIENT_USER.USER_ROLE))
                .leftJoin(ISSUER_USER).on(ISSUER_USER.ID.eq(ISSUE.CREATED_BY));

        var finalQuery = joiningStrategy.joinEntity(baseQuery, entityId);

        return finalQuery.orderBy(ISSUE.CREATED_AT)
                .fetch(record -> {
                    var recipientInfo = new IssueResponse.IssueRecipientInfo(
                            record.get("recipient_fn", String.class),
                            record.get("recipient_ln", String.class),
                            record.get("recipient_role_val") != null
                                    ? UserRole.valueOf(record.get("recipient_role_val", String.class))
                                    : null
                    );

                    var issuerInfo = new IssueResponse.IssueIssuerInfo(
                            record.get("issuer_fn", String.class),
                            record.get("issuer_ln", String.class)
                    );

                    return new IssueResponse(
                            record.get(ISSUE.ID),
                            record.get(ISSUE.TITLE),
                            record.get(ISSUE.DESCRIPTION),
                            IssueProcessingStatus.valueOf(record.get(ISSUE.STATUS)),
                            IssuePriority.valueOf(record.get(ISSUE.PRIORITY)),
                            record.get(NOTIFICATION.SEEN_AT),
                            recipientInfo,
                            issuerInfo
                    );
                });
    }

    public void updateStatus(UUID issueId, IssueProcessingStatus updatedStatus) {
        dslContext.update(ISSUE)
                .set(ISSUE.STATUS, updatedStatus.name())
                .where(ISSUE.ID.eq(issueId))
                .execute();

    }

    public List<IssueSimpleResponse> getIssues(UUID id) {
        return dslContext.select(
                ISSUE.ID,
                ISSUE.TITLE,
                ISSUE.DESCRIPTION,
                ISSUE.STATUS,
                ISSUE.PRIORITY
        )
                .from(ISSUE)
                .where(ISSUE.CREATED_BY.eq(id))
                .fetch(record -> new IssueSimpleResponse(
                        record.get(ISSUE.ID),
                        record.get(ISSUE.TITLE),
                        record.get(ISSUE.DESCRIPTION),
                        IssueProcessingStatus.valueOf(record.get(ISSUE.STATUS)),
                        IssuePriority.valueOf(record.get(ISSUE.PRIORITY))
                ));
    }
}
