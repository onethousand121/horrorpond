package com.horrorpond.ingestion.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("ingestion")
public record IngestionProperties(
        @DefaultValue("https://store.steampowered.com") String steamStoreBaseUrl,
        @DefaultValue("https://steamspy.com") String steamSpyBaseUrl,
        @DefaultValue("1500ms") Duration requestInterval,
        @DefaultValue("60s") Duration rateLimitWait,
        @DefaultValue("3") int maxConsecutiveRateLimits,
        @DefaultValue Enrichment enrichment,
        @DefaultValue Scheduler scheduler
) {

    public record Enrichment(
            @DefaultValue("500") int maxPerRun,
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
