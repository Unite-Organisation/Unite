package com.app.prod.mail.repository;

import com.app.prod.mail.dto.DeliverySummary;
import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.mail.enums.EmailDeliveryType;
import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.Select;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;
import static org.jooq.sources.Tables.EMAIL_DELIVERY;

public class EmailDeliveryFields {

    public static Field<DeliverySummary> lastDelivery(Field<UUID> userId, EmailDeliveryType type) {
        return multiset(lastDeliveryOf(userId, type))
                .as("last_delivery")
                .convertFrom(result -> result.isEmpty() ? null : new DeliverySummary(
                        EmailDeliveryStatus.valueOf(result.getFirst().get(EMAIL_DELIVERY.STATUS)),
                        result.getFirst().get(EMAIL_DELIVERY.ERROR_MESSAGE),
                        result.getFirst().get(EMAIL_DELIVERY.CREATED_AT),
                        result.getFirst().get(EMAIL_DELIVERY.SENT_AT)
                ));
    }

    public static Field<String> lastDeliveryStatus(Field<UUID> userId, EmailDeliveryType type) {
        return field(select(EMAIL_DELIVERY.STATUS)
                .from(EMAIL_DELIVERY)
                .where(EMAIL_DELIVERY.USER_ID.eq(userId))
                .and(EMAIL_DELIVERY.DELIVERY_TYPE.eq(type.name()))
                .orderBy(EMAIL_DELIVERY.CREATED_AT.desc())
                .limit(1));
    }

    private static Select<Record4<String, String, LocalDateTime, LocalDateTime>> lastDeliveryOf(
            Field<UUID> userId, EmailDeliveryType type
    ) {
        return select(
                EMAIL_DELIVERY.STATUS,
                EMAIL_DELIVERY.ERROR_MESSAGE,
                EMAIL_DELIVERY.CREATED_AT,
                EMAIL_DELIVERY.SENT_AT
        )
                .from(EMAIL_DELIVERY)
                .where(EMAIL_DELIVERY.USER_ID.eq(userId))
                .and(EMAIL_DELIVERY.DELIVERY_TYPE.eq(type.name()))
                .orderBy(EMAIL_DELIVERY.CREATED_AT.desc())
                .limit(1);
    }
}
