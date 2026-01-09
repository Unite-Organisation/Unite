package com.app.prod.issues.repository;

import com.app.prod.issues.dto.NotificationResponse;
import com.app.prod.issues.enums.IssueObject;
import com.app.prod.issues.enums.IssuePriority;
import com.app.prod.issues.enums.IssueProcessingStatus;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.AppUser;
import org.jooq.sources.tables.Notification;
import org.jooq.sources.tables.UserRole;
import org.jooq.sources.tables.records.NotificationRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
        AppUser RECIPIENT = APP_USER.as("recipient");
        AppUser ISSUER = APP_USER.as("issuer");
        UserRole ISSUER_ROLE = USER_ROLE.as("issuer_role");

        return dslContext.select(
                NOTIFICATION.ID,
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
                ISSUER.ID,
                ISSUER.FIRST_NAME,
                ISSUER.LAST_NAME,
                ISSUER.USERNAME,
                ISSUER_ROLE.USER_ROLE_,
                ISSUE.CREATED_AT
        )
                .from(ISSUE)
                .leftJoin(NOTIFICATION).on(NOTIFICATION.ISSUE_ID.eq(ISSUE.ID))
                .leftJoin(RECIPIENT).on(NOTIFICATION.RECIPIENT_ID.eq(RECIPIENT.ID))
                .leftJoin(ISSUER).on(ISSUE.CREATED_BY.eq(ISSUER.ID))
                .leftJoin(ISSUER_ROLE).on(ISSUER.USER_ROLE.eq(ISSUER_ROLE.ID))
                .leftJoin(BUILDING).on(ISSUE.BUILDING_ID.eq(BUILDING.ID))
                .leftJoin(AREA).on(ISSUE.AREA_ID.eq(AREA.ID))
                .leftJoin(FACILITY).on(ISSUE.FACILITY_ID.eq(FACILITY.ID))
                .leftJoin(POLL).on(ISSUE.POLL_ID.eq(POLL.ID))
                .where(RECIPIENT.ID.eq(managerId))
                .fetch(record -> {

                    NotificationResponse.IssuerData issuerData = new NotificationResponse.IssuerData(
                            record.get(ISSUER.ID),
                            record.get(ISSUER.FIRST_NAME),
                            record.get(ISSUER.LAST_NAME),
                            record.get(ISSUER.USERNAME),
                            record.get(ISSUER_ROLE.USER_ROLE_),
                            record.get(ISSUE.CREATED_AT)
                    );

                    return new NotificationResponse(
                            record.get(NOTIFICATION.ID),
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

    public UUID updateSeenAtDateReturnIssueId(UUID managerId, UUID notificationId, LocalDateTime now) {
        return dslContext.update(NOTIFICATION)
                .set(NOTIFICATION.SEEN_AT, now)
                .where(NOTIFICATION.RECIPIENT_ID.eq(managerId))
                .and(NOTIFICATION.ID.eq(notificationId))
                .returning(NOTIFICATION.ISSUE_ID)
                .fetchOne(NOTIFICATION.ISSUE_ID);
    }
}
