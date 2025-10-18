package com.app.prod.issues.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Notification;
import org.jooq.sources.tables.records.NotificationRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static org.jooq.sources.Tables.NOTIFICATION;

@Repository
public class NotificationRepository extends BaseJooqRepository<Notification, NotificationRecord, UUID> {
    protected NotificationRepository(DSLContext dsl) {
        super(dsl, NOTIFICATION, NOTIFICATION.ID);
    }
}
