package com.horrorpond.ingestion.client;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ThreadSleeper implements Sleeper {

    @Override
    public void sleep(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sleeping", e);
        }
    }
}
