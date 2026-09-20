package com.app.prod.event.mappers;

import com.app.prod.event.dto.CreateEventRequest;
import com.app.prod.event.dto.MemberResponse;
import com.app.prod.event.enums.EventIdentityOrigin;
import com.app.prod.event.enums.EventMemberRole;
import com.app.prod.event.enums.EventMemberStatus;
import org.jooq.sources.tables.records.AppUserRecord;
import org.jooq.sources.tables.records.EventMemberRecord;
import org.jooq.sources.tables.records.EventRecord;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class EventMapper {

    public static EventRecord fromRequestToRecord(CreateEventRequest request, UUID id, String slug, LocalDateTime now) {
        EventRecord record = new EventRecord();

        record.setId(id);
        record.setPublicSlug(slug);
        record.setName(request.name());
        record.setDescription(request.description());
        record.setStartDateTime(request.startDate());
        record.setEndDateTime(request.endDate());
        record.setLocationName(request.location());
        record.setOnlineUrl(request.onlineUrl());
        record.setMaxAttendees(request.maxAttendees());
        record.setWaitlistEnabled(request.waitlistEnabled());
        record.setCreatedAt(now);

        return record;
    }

    public static EventMemberRecord uniteMember(UUID eventId, AppUserRecord user, EventMemberRole role, EventMemberStatus status, LocalDateTime now) {
        EventMemberRecord record = baseMember(eventId, displayName(user), role, status, now);
        record.setUserId(user.getId());
        return record;
    }

    public static EventMemberRecord guestMember(UUID eventId, String displayName, String pinHash, EventMemberRole role, EventMemberStatus status, LocalDateTime now) {
        EventMemberRecord record = baseMember(eventId, displayName.trim(), role, status, now);
        record.setPinHash(pinHash);
        return record;
    }

    public static MemberResponse toResponse(EventMemberRecord record) {
        return new MemberResponse(
                record.getDisplayName(),
                EventMemberRole.valueOf(record.getRole()),
                EventMemberStatus.valueOf(record.getStatus()),
                record.getUserId() != null ? EventIdentityOrigin.UNITE : EventIdentityOrigin.GUEST
        );
    }

    private static EventMemberRecord baseMember(UUID eventId, String displayName, EventMemberRole role, EventMemberStatus status, LocalDateTime now) {
        EventMemberRecord record = new EventMemberRecord();

        record.setId(UUID.randomUUID());
        record.setEventId(eventId);
        record.setDisplayName(displayName);
        record.setRole(role.name());
        record.setStatus(status.name());
        record.setStatusChangedAt(now);
        record.setCreatedAt(now);

        return record;
    }

    private static String displayName(AppUserRecord user) {
        String fullName = String.join(" ",
                Objects.requireNonNullElse(user.getFirstName(), ""),
                Objects.requireNonNullElse(user.getLastName(), "")
        ).trim();
        return fullName.isEmpty() ? user.getUsername() : fullName;
    }
}
