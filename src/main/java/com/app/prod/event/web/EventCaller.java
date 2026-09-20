package com.app.prod.event.web;

import org.jooq.sources.tables.records.AppUserRecord;

import java.util.Optional;

public final class EventCaller {

    private final AppUserRecord user;

    EventCaller(AppUserRecord user) {
        this.user = user;
    }

    public Optional<AppUserRecord> uniteUser() {
        return Optional.ofNullable(user);
    }

    public boolean isGuest() {
        return user == null;
    }
}
