package com.horrorpond.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {

    private Instant now;

    public MutableClock(Instant now) {
        this.now = now;
    }

    public synchronized void advance(Duration duration) {
        now = now.plus(duration);
    }

    public synchronized void set(Instant instant) {
        now = instant;
    }

    @Override
    public synchronized Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }
}
