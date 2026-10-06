package com.horrorpond.ingestion.client;

import com.horrorpond.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SteamRequestPacerTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final List<Duration> slept = new ArrayList<>();
    private final Sleeper sleeper = duration -> {
        slept.add(duration);
        clock.advance(duration);
    };
    private final SteamRequestPacer pacer = new SteamRequestPacer(clock, sleeper, Duration.ofMillis(1500));

    @Test
    void firstCallDoesNotWait() {
        pacer.acquire();

        assertThat(slept).isEmpty();
    }

    @Test
    void consecutiveCallWaitsOnlyForRemainingInterval() {
        pacer.acquire();
        clock.advance(Duration.ofMillis(400));

        pacer.acquire();

        assertThat(slept).containsExactly(Duration.ofMillis(1100));
    }

    @Test
    void immediateConsecutiveCallsWaitFullIntervalEachTime() {
        pacer.acquire();
        pacer.acquire();
        pacer.acquire();

        assertThat(slept).containsExactly(Duration.ofMillis(1500), Duration.ofMillis(1500));
    }

    @Test
    void noWaitWhenIntervalAlreadyPassed() {
        pacer.acquire();
        clock.advance(Duration.ofSeconds(2));

        pacer.acquire();

        assertThat(slept).isEmpty();
    }
}
