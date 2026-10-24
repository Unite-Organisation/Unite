package com.app.prod.event.repository;

import com.app.prod.event.dto.SlotTally;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.sources.tables.EventSlot;
import org.jooq.sources.tables.records.EventSlotRecord;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.jooq.impl.DSL.count;
import static org.jooq.sources.Tables.EVENT_MEMBER;
import static org.jooq.sources.Tables.EVENT_SLOT;
import static org.jooq.sources.Tables.EVENT_SLOT_VOTE;

@Repository
public class EventSlotRepository extends BaseJooqRepository<EventSlot, EventSlotRecord, UUID> {

    protected EventSlotRepository(DSLContext dsl) {
        super(dsl, EVENT_SLOT, EVENT_SLOT.ID);
    }

    public List<EventSlotRecord> findByEvent(UUID eventId) {
        return dslContext.selectFrom(EVENT_SLOT)
                .where(EVENT_SLOT.EVENT_ID.eq(eventId))
                .orderBy(EVENT_SLOT.START_DATE_TIME)
                .fetch();
    }

    public boolean allBelongToEvent(UUID eventId, Collection<UUID> slotIds) {
        Set<UUID> distinct = Set.copyOf(slotIds);
        if (distinct.isEmpty()) {
            return true;
        }
        return dslContext.fetchCount(EVENT_SLOT,
                EVENT_SLOT.EVENT_ID.eq(eventId).and(EVENT_SLOT.ID.in(distinct))) == distinct.size();
    }

    public List<SlotTally> tallies(UUID eventId) {
        Field<Integer> preferred = count(EVENT_MEMBER.ID)
                .filterWhere(EVENT_SLOT_VOTE.PREFERENCE.eq(SlotPreference.PREFERRED.name()))
                .as("preferred");
        Field<Integer> ifNeeded = count(EVENT_MEMBER.ID)
                .filterWhere(EVENT_SLOT_VOTE.PREFERENCE.eq(SlotPreference.IF_NEEDED.name()))
                .as("if_needed");

        return dslContext.select(
                        EVENT_SLOT.ID,
                        EVENT_SLOT.START_DATE_TIME,
                        preferred,
                        ifNeeded
                )
                .from(EVENT_SLOT)
                .leftJoin(EVENT_SLOT_VOTE).on(EVENT_SLOT_VOTE.SLOT_ID.eq(EVENT_SLOT.ID))
                .leftJoin(EVENT_MEMBER).on(EVENT_MEMBER.ID.eq(EVENT_SLOT_VOTE.MEMBER_ID)
                        .and(EVENT_MEMBER.STATUS.eq(EventMemberStatus.GOING.name())))
                .where(EVENT_SLOT.EVENT_ID.eq(eventId))
                .groupBy(EVENT_SLOT.ID, EVENT_SLOT.START_DATE_TIME)
                .orderBy(EVENT_SLOT.START_DATE_TIME)
                .fetch(record -> new SlotTally(
                        record.get(EVENT_SLOT.ID),
                        record.get(EVENT_SLOT.START_DATE_TIME),
                        record.get(preferred),
                        record.get(ifNeeded)
                ));
    }
}
