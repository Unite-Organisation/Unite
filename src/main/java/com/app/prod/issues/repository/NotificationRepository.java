package com.app.prod.issues.repository;

import com.app.prod.issues.dto.NotificationResponse;
import com.app.prod.issues.enums.IssueObject;
import com.app.prod.issues.enums.IssuePriority;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Notification;
import org.jooq.sources.tables.records.NotificationRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.coalesce;
import static org.jooq.sources.Tables.*;

@Repository
public class NotificationRepository extends BaseJooqRepository<Notification, NotificationRecord, UUID> {
    protected NotificationRepository(DSLContext dsl) {
        super(dsl, NOTIFICATION, NOTIFICATION.ID);
    }

    public List<NotificationResponse> getNotifications(UUID managerId) {
        return dslContext.select(
                ISSUE.ID,
                ISSUE.TITLE,
                ISSUE.DESCRIPTION,
                ISSUE.STATUS,
                ISSUE.PRIORITY,
                ISSUE.ISSUE_OBJECT,
                coalesce(
                        ISSUE.BUILDING_ID,
                        ISSUE.AREA_ID,
                        ISSUE.FACILITY_ID,
                        ISSUE.POLL_ID
                ).as("entityId"),
                USERS.ID,
                USERS.FIRST_NAME,
                USERS.LAST_NAME,
                USERS.USERNAME,
                USER_ROLES.USER_ROLE,
                ISSUE.CREATED_AT
        )
                .from(ISSUE)
                .leftJoin(NOTIFICATION).on(NOTIFICATION.ISSUE_ID.eq(ISSUE.ID))
                .leftJoin(USERS).on(NOTIFICATION.RECIPIENT_ID.eq(USERS.ID))
                .leftJoin(USER_ROLES).on(USERS.USER_ROLE.eq(USER_ROLES.ID))
                .leftJoin(BUILDINGS).on(ISSUE.BUILDING_ID.eq(BUILDINGS.ID))
                .leftJoin(AREAS).on(ISSUE.AREA_ID.eq(AREAS.ID))
                .leftJoin(FACILITIES).on(ISSUE.FACILITY_ID.eq(FACILITIES.ID))
                .leftJoin(POLLS).on(ISSUE.POLL_ID.eq(POLLS.ID))
                .where(USERS.ID.eq(managerId))
                .fetch(record -> {

                    NotificationResponse.IssuerData issuerData = new NotificationResponse.IssuerData(
                            record.get(USERS.ID),
                            record.get(USERS.FIRST_NAME),
                            record.get(USERS.LAST_NAME),
                            record.get(USERS.USERNAME),
                            record.get(USER_ROLES.USER_ROLE),
                            record.get(ISSUE.CREATED_AT)
                    );

                    return new NotificationResponse(
                            record.get(ISSUE.ID),
                            record.get(ISSUE.TITLE),
                            record.get(ISSUE.DESCRIPTION),
                            IssueProcessingStatus.valueOf(record.get(ISSUE.STATUS)),
                            IssuePriority.valueOf(record.get(ISSUE.PRIORITY)),
                            IssueObject.valueOf(record.get(ISSUE.ISSUE_OBJECT)),
                            (UUID) record.get("entityId"),
                            issuerData
                    );
                });
    }
}
