package com.app.prod.mail.repository;

import com.app.prod.mail.dto.EmailDeliveryResponse;
import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.EmailDelivery;
import org.jooq.sources.tables.records.EmailDeliveryRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.noCondition;
import static org.jooq.sources.tables.EmailDelivery.EMAIL_DELIVERY;

@Repository
public class EmailDeliveryRepository extends BaseJooqRepository<EmailDelivery, EmailDeliveryRecord, UUID> {

    protected EmailDeliveryRepository(DSLContext dsl) {
        super(dsl, EMAIL_DELIVERY, EMAIL_DELIVERY.ID);
    }

    public void markSent(UUID deliveryId, LocalDateTime sentAt) {
        dslContext.update(EMAIL_DELIVERY)
                .set(EMAIL_DELIVERY.STATUS, EmailDeliveryStatus.SENT.name())
                .set(EMAIL_DELIVERY.SENT_AT, sentAt)
                .where(EMAIL_DELIVERY.ID.eq(deliveryId))
                .execute();
    }

    public void markFailed(UUID deliveryId, String errorMessage) {
        dslContext.update(EMAIL_DELIVERY)
                .set(EMAIL_DELIVERY.STATUS, EmailDeliveryStatus.FAILED.name())
                .set(EMAIL_DELIVERY.ERROR_MESSAGE, errorMessage)
                .where(EMAIL_DELIVERY.ID.eq(deliveryId))
                .execute();
    }

    public List<EmailDeliveryResponse> findDeliveries(EmailDeliveryStatus status) {
        var statusCondition = (status != null)
                ? EMAIL_DELIVERY.STATUS.eq(status.name())
                : noCondition();

        return dslContext.selectFrom(EMAIL_DELIVERY)
                .where(statusCondition)
                .orderBy(EMAIL_DELIVERY.CREATED_AT.desc())
                .fetch(record -> new EmailDeliveryResponse(
                        record.get(EMAIL_DELIVERY.ID),
                        EmailDeliveryType.valueOf(record.get(EMAIL_DELIVERY.DELIVERY_TYPE)),
                        record.get(EMAIL_DELIVERY.REFERENCE_ID),
                        record.get(EMAIL_DELIVERY.EMAIL),
                        EmailDeliveryStatus.valueOf(record.get(EMAIL_DELIVERY.STATUS)),
                        record.get(EMAIL_DELIVERY.ERROR_MESSAGE),
                        record.get(EMAIL_DELIVERY.CREATED_AT),
                        record.get(EMAIL_DELIVERY.SENT_AT)
                ));
    }
}
