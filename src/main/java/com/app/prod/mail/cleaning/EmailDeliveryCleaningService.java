package com.app.prod.mail.cleaning;

import com.app.prod.cleaning.DatabaseEntityCleaningPolicy;
import com.app.prod.mail.enums.EmailDeliveryStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.EMAIL_DELIVERY;

@Component
public class EmailDeliveryCleaningService extends DatabaseEntityCleaningPolicy {
    protected EmailDeliveryCleaningService(DSLContext dslContext) {
        super(dslContext, EMAIL_DELIVERY, EMAIL_DELIVERY.ID);
    }

    @Override
    public String getEntityName() {
        return "email_delivery";
    }

    @Override
    public Duration getRetentionPeriod() {
        return Duration.ofDays(30);
    }

    @Override
    protected List<UUID> findExpiredEntityIds(LocalDateTime expirationCutoff) {
        // it appears to be a problem ith removing entities with SENT status as they are used somewhere else
        return List.of();

//        return dslContext.select(EMAIL_DELIVERY.ID)
//                .from(EMAIL_DELIVERY)
//                .where(EMAIL_DELIVERY.STATUS.eq(EmailDeliveryStatus.SENT.name()))
//                .and(EMAIL_DELIVERY.SENT_AT.le(expirationCutoff))
//                .fetch(EMAIL_DELIVERY.ID);
    }

    @Override
    protected void deleteRecordsNotLinkedByForeignKey(List<UUID> deletedEntityIds) {

    }
}
