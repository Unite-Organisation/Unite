package com.app.prod.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public MutableClock(Instant initialInstant, ZoneId zone) {
        this.instant = initialInstant;
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    public void setInstant(Instant newInstant) {
        this.instant = newInstant;
    }

    public void advance(Duration duration) {
        this.instant = this.instant.plus(duration);
    }
}