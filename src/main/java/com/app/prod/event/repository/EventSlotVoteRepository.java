package com.app.prod.event.repository;

import com.app.prod.event.dto.SlotVote;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.EventSlotVote;
import org.jooq.sources.tables.records.EventSlotVoteRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.EVENT_SLOT_VOTE;

@Repository
public class EventSlotVoteRepository extends BaseJooqRepository<EventSlotVote, EventSlotVoteRecord, UUID> {

    protected EventSlotVoteRepository(DSLContext dsl) {
        super(dsl, EVENT_SLOT_VOTE, EVENT_SLOT_VOTE.ID);
    }

    public List<SlotVote> findByMember(UUID memberId) {
        return dslContext.select(EVENT_SLOT_VOTE.SLOT_ID, EVENT_SLOT_VOTE.PREFERENCE)
                .from(EVENT_SLOT_VOTE)
                .where(EVENT_SLOT_VOTE.MEMBER_ID.eq(memberId))
                .fetch(record -> new SlotVote(
                        record.get(EVENT_SLOT_VOTE.SLOT_ID),
                        SlotPreference.valueOf(record.get(EVENT_SLOT_VOTE.PREFERENCE))
                ));
    }

    public void replaceForMember(UUID memberId, Collection<SlotVote> votes, LocalDateTime now) {
        dslContext.deleteFrom(EVENT_SLOT_VOTE)
                .where(EVENT_SLOT_VOTE.MEMBER_ID.eq(memberId))
                .execute();

        if (votes.isEmpty()) {
            return;
        }

        List<EventSlotVoteRecord> records = votes.stream()
                .map(vote -> {
                    EventSlotVoteRecord record = new EventSlotVoteRecord();
                    record.setId(UUID.randomUUID());
                    record.setSlotId(vote.slotId());
                    record.setMemberId(memberId);
                    record.setPreference(vote.preference().name());
                    record.setCreatedAt(now);
                    return record;
                })
                .toList();

        dslContext.batchInsert(records).execute();
    }
}
