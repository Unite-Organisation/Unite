package com.app.prod.event.cleaning;

import com.app.prod.cleaning.DatabaseEntityCleaningPolicy;
import com.app.prod.event.enums.EventStatus;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Related.notExistsIn;
import static org.jooq.sources.Tables.EVENT;
import static org.jooq.sources.Tables.POST;

@Component
public class EventCleaningPolicy extends DatabaseEntityCleaningPolicy {
    public EventCleaningPolicy(DSLContext dslContext) {
        super(dslContext, EVENT, EVENT.ID);
    }

    @Override
    public String getEntityName() {
        return "event";
    }

    @Override
    public Duration getRetentionPeriod() {
        return Duration.ofDays(30);
    }

    @Override
    protected List<UUID> findExpiredEntityIds(LocalDateTime expirationCutoff) {
        return dslContext.select(EVENT.ID)
                .from(EVENT)
                .where(notExistsIn(POST, POST.EVENT_ID.eq(EVENT.ID)))
                .and(momentTheEventStoppedMattering().lt(expirationCutoff))
                .fetch(EVENT.ID);
    }

    private static Field<LocalDateTime> momentTheEventStoppedMattering() {
        List<String> STATUSES_STILL_VOTING_FOR_A_DATE = List.of(EventStatus.COLLECTING_VOTES.name(), EventStatus.ONE_SLOT_LEFT.name());
        return DSL.coalesce(
                DSL.when(EVENT.STATUS.in(STATUSES_STILL_VOTING_FOR_A_DATE), EVENT.VOTING_DEADLINE)
                        .when(EVENT.STATUS.eq(EventStatus.GROUP_FORMED.name()), EVENT.CONFIRM_BY)
                        .else_(DSL.coalesce(EVENT.END_DATE_TIME, EVENT.START_DATE_TIME)),
                EVENT.CREATED_AT
        );
    }

    @Override
    protected void deleteRecordsNotLinkedByForeignKey(List<UUID> deletedEntityIds) {

    }
}
