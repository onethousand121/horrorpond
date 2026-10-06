package com.horrorpond.ingestion.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Steam Store 호출 간격을 고정 간격(requestInterval) 이상으로 유지한다.
 * 마지막 호출 이후 간격이 덜 지났으면 남은 시간만큼 대기한다.
 */
@Component
public class SteamRequestPacer {

    private final Clock clock;
    private final Sleeper sleeper;
    private final Duration interval;

    private Instant lastRequestAt;

    @Autowired
    public SteamRequestPacer(Clock clock, Sleeper sleeper, IngestionProperties properties) {
        this(clock, sleeper, properties.requestInterval());
    }

    SteamRequestPacer(Clock clock, Sleeper sleeper, Duration interval) {
        this.clock = clock;
        this.sleeper = sleeper;
        this.interval = interval;
    }

    public synchronized void acquire() {
        if (lastRequestAt != null) {
            Duration elapsed = Duration.between(lastRequestAt, clock.instant());
            Duration remaining = interval.minus(elapsed);
            if (remaining.compareTo(Duration.ZERO) > 0) {
                sleeper.sleep(remaining);
            }
        }
        lastRequestAt = clock.instant();
    }
}
