package com.horrorpond.ingestion.client;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("ingestion")
public record IngestionProperties(
        @DefaultValue("https://store.steampowered.com") String steamStoreBaseUrl,
        @DefaultValue("https://steamspy.com") String steamSpyBaseUrl,
        @DefaultValue("1500ms") Duration requestInterval,
        @DefaultValue("60s") Duration rateLimitWait,
        @DefaultValue("3") int maxConsecutiveRateLimits,
        @DefaultValue("PT2H") Duration lockAtMostFor,
        @DefaultValue Enrichment enrichment,
        @DefaultValue Scheduler scheduler
) {

    static final double MAX_RUN_TO_LOCK_RATIO = 0.8;

    /**
     * 한 번의 enrichment가 락 유지 시간 안에 끝나야 한다. 그렇지 않으면 실행 중에 락이 풀려
     * 다른 인스턴스가 같은 작업을 동시에 시작할 수 있다.
     */
    @AssertTrue(message = "enrichment.max-per-run x request-interval must not exceed 80% of lock-at-most-for")
    public boolean isEnrichmentRunWithinLock() {
        return estimatedEnrichmentDuration().toMillis() <= lockAtMostFor.toMillis() * MAX_RUN_TO_LOCK_RATIO;
    }

    public Duration estimatedEnrichmentDuration() {
        return requestInterval.multipliedBy(enrichment.maxPerRun());
    }

    public record Enrichment(
            @DefaultValue("2000") int maxPerRun,
            @DefaultValue("7d") Duration refreshAfter,
            @DefaultValue("1d") Duration failedRetryAfter,
            @DefaultValue("3") int maxFailCount
    ) {
    }

    public record Scheduler(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("0 0 4 * * *") String cron,
            @DefaultValue("Asia/Seoul") String zone
    ) {
    }
}
