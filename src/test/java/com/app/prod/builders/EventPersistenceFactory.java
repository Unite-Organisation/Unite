package com.app.prod.builders;

import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import com.app.prod.event.enums.EventStatus;
import com.app.prod.event.enums.SchedulingMode;
import com.app.prod.event.enums.SlotPreference;
import com.app.prod.event.repository.EventMemberRepository;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.event.repository.EventSlotRepository;
import com.app.prod.event.repository.EventSlotVoteRepository;
import com.app.prod.event.service.EventSlug;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventRecord;
import org.jooq.sources.tables.records.EventSlotRecord;
import org.jooq.sources.tables.records.EventSlotVoteRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventPersistenceFactory {

    private final Clock clock;
    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final EventSlotRepository eventSlotRepository;
    private final EventSlotVoteRepository eventSlotVoteRepository;

    public EventBuilder getNewEvent() {
        return new EventBuilder();
    }

    public MemberBuilder getNewMember() {
        return new MemberBuilder();
    }

    public SlotBuilder getNewSlot() {
        return new SlotBuilder();
    }

    public VoteBuilder getNewVote() {
        return new VoteBuilder();
    }

    public class EventBuilder {

        private final EventRecord instance = new EventRecord();

        public EventBuilder id(UUID id) {
            instance.setId(id);
            return this;
        }

        public EventBuilder name(String name) {
            instance.setName(name);
            return this;
        }

        public EventBuilder maxAttendees(Integer maxAttendees) {
            instance.setMaxAttendees(maxAttendees);
            return this;
        }

        public EventBuilder waitlistEnabled(boolean waitlistEnabled) {
            instance.setWaitlistEnabled(waitlistEnabled);
            return this;
        }

        public EventBuilder status(EventStatus status) {
            instance.setStatus(status.name());
            return this;
        }

        public EventBuilder selectedSlotId(UUID selectedSlotId) {
            instance.setSelectedSlotId(selectedSlotId);
            return this;
        }

        public EventBuilder confirmBy(LocalDateTime confirmBy) {
            instance.setConfirmBy(confirmBy);
            return this;
        }

        public EventBuilder startDateTime(LocalDateTime startDateTime) {
            instance.setStartDateTime(startDateTime);
            return this;
        }

        /** A dated event - what every event was before slots existed. */
        public EventBuilder fixed() {
            withDefaults();
            instance.setSchedulingMode(SchedulingMode.FIXED.name());
            instance.setStatus(EventStatus.CONFIRMED.name());
            instance.setStartDateTime(LocalDateTime.now(clock).plusDays(7));
            return this;
        }

        /** An event still looking for its date. */
        public EventBuilder poll(int minAttendees, LocalDateTime votingDeadline) {
            withDefaults();
            instance.setSchedulingMode(SchedulingMode.POLL.name());
            instance.setStatus(EventStatus.COLLECTING.name());
            instance.setMinAttendees(minAttendees);
            instance.setVotingDeadline(votingDeadline);
            return this;
        }

        private void withDefaults() {
            instance.setId(UUID.randomUUID());
            instance.setPublicSlug(EventSlug.generate());
            instance.setName("Volleyball");
            instance.setWaitlistEnabled(false);
            instance.setCreatedAt(LocalDateTime.now(clock));
        }

        public EventRecord build() {
            return instance;
        }

        public EventRecord buildAndSave() {
            EventRecord record = build();
            eventRepository.insertOne(record);
            return record;
        }
    }

    public class MemberBuilder {

        private final EventMemberRecord instance = new EventMemberRecord();

        public MemberBuilder eventId(UUID eventId) {
            instance.setEventId(eventId);
            return this;
        }

        public MemberBuilder displayName(String displayName) {
            instance.setDisplayName(displayName);
            return this;
        }

        public MemberBuilder role(EventMemberRole role) {
            instance.setRole(role.name());
            return this;
        }

        public MemberBuilder status(EventMemberStatus status) {
            instance.setStatus(status.name());
            return this;
        }

        public MemberBuilder userId(UUID userId) {
            instance.setUserId(userId);
            instance.setPinHash(null);
            return this;
        }

        /** A guest: the identity check wants a pin hash exactly when there is no account behind it. */
        public MemberBuilder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setDisplayName("guest-" + UUID.randomUUID().toString().substring(0, 8));
            instance.setRole(EventMemberRole.MEMBER.name());
            instance.setStatus(EventMemberStatus.GOING.name());
            instance.setPinHash("$2a$10$testonlyhashtestonlyhashtestonlyhashtestonlyhashtesto");
            instance.setFailedAttempts(0);
            instance.setStatusChangedAt(LocalDateTime.now(clock));
            instance.setCreatedAt(LocalDateTime.now(clock));
            return this;
        }

        public EventMemberRecord build() {
            return instance;
        }

        public EventMemberRecord buildAndSave() {
            EventMemberRecord record = build();
            eventMemberRepository.insertOne(record);
            return record;
        }
    }

    public class SlotBuilder {

        private final EventSlotRecord instance = new EventSlotRecord();

        public SlotBuilder eventId(UUID eventId) {
            instance.setEventId(eventId);
            return this;
        }

        public SlotBuilder startDateTime(LocalDateTime startDateTime) {
            instance.setStartDateTime(startDateTime);
            return this;
        }

        public SlotBuilder endDateTime(LocalDateTime endDateTime) {
            instance.setEndDateTime(endDateTime);
            return this;
        }

        public SlotBuilder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setStartDateTime(LocalDateTime.now(clock).plusDays(7));
            instance.setCreatedAt(LocalDateTime.now(clock));
            return this;
        }

        public EventSlotRecord build() {
            return instance;
        }

        public EventSlotRecord buildAndSave() {
            EventSlotRecord record = build();
            eventSlotRepository.insertOne(record);
            return record;
        }
    }

    public class VoteBuilder {

        private final EventSlotVoteRecord instance = new EventSlotVoteRecord();

        public VoteBuilder slotId(UUID slotId) {
            instance.setSlotId(slotId);
            return this;
        }

        public VoteBuilder memberId(UUID memberId) {
            instance.setMemberId(memberId);
            return this;
        }

        public VoteBuilder preference(SlotPreference preference) {
            instance.setPreference(preference.name());
            return this;
        }

        public VoteBuilder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setPreference(SlotPreference.PREFERRED.name());
            instance.setCreatedAt(LocalDateTime.now(clock));
            return this;
        }

        public EventSlotVoteRecord build() {
            return instance;
        }

        public EventSlotVoteRecord buildAndSave() {
            EventSlotVoteRecord record = build();
            eventSlotVoteRepository.insertOne(record);
            return record;
        }
    }
}
