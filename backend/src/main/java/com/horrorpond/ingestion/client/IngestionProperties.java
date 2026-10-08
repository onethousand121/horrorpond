package com.horrorpond.ingestion.client;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
        @DefaultValue("1s") Duration steamSpyRequestInterval,
        @DefaultValue("60s") Duration rateLimitWait,
        @DefaultValue("3") int maxConsecutiveRateLimits,
        @DefaultValue("PT2H") Duration lockAtMostFor,
        @DefaultValue Enrichment enrichment,
        @DefaultValue Metrics metrics,
        @DefaultValue Scheduler scheduler
) {

    static final double MAX_RUN_TO_LOCK_RATIO = 0.8;

    /**
     * 한 번의 수집(enrichment + 리뷰 수 기록)이 락 유지 시간 안에 끝나야 한다. 그렇지 않으면 실행 중에 락이 풀려
     * 다른 인스턴스가 같은 작업을 동시에 시작할 수 있다.
     */
    @AssertTrue(message = "enrichment.max-per-run x (2 x request-interval + steam-spy-request-interval)"
            + " + metrics.max-per-run x request-interval must not exceed 80% of lock-at-most-for")
    public boolean isEnrichmentRunWithinLock() {
        return estimatedEnrichmentDuration().plus(estimatedMetricsDuration()).compareTo(runTimeBudget()) <= 0;
    }

    /**
     * 한 번의 실행이 쓸 수 있는 최대 시간 (lockAtMostFor의 80%). 기동 검증과 실행 중 예산 확인이 함께 쓴다.
     */
    public Duration runTimeBudget() {
        return Duration.ofMillis((long) (lockAtMostFor.toMillis() * MAX_RUN_TO_LOCK_RATIO));
    }

    /**
     * enrichment가 쓸 수 있는 시간. 뒤에 이어지는 리뷰 수 기록(METRICS) 몫을 남겨 둔다.
     */
    public Duration enrichmentTimeBudget() {
        return runTimeBudget().minus(estimatedMetricsDuration());
    }

    /**
     * 게임마다 appreviews를 1번 호출한다.
     */
    public Duration estimatedMetricsDuration() {
        return requestInterval.multipliedBy(metrics.maxPerRun());
    }

    /**
     * 아이템마다 SteamSpy 태그 판정 + Steam appdetails(한국어, 영어)를 호출한다.
     * 리뷰 수 보충(appreviews)은 일부 게임만 하므로 넣지 않고, 실행 중 예산 확인(runTimeBudget)이 막는다.
     */
    public Duration estimatedEnrichmentDuration() {
        return requestInterval.multipliedBy(2).plus(steamSpyRequestInterval).multipliedBy(enrichment.maxPerRun());
    }

    public record Enrichment(
            @DefaultValue("1200") int maxPerRun,
            @DefaultValue("7d") Duration refreshAfter,
            @DefaultValue("1d") Duration failedRetryAfter,
            @DefaultValue("3") int maxFailCount
    ) {
    }

    /**
     * 하루 1회 Steam 리뷰 수 기록(트렌드용).
     *
     * @param maxPerRun  한 번에 기록할 최대 게임 수 (0이면 끔)
     * @param recentDays 이 기간 안에 출시한 게임을 먼저 기록한다
     * @param minReviews 최근 출시작이 아니면 리뷰가 이만큼 이상인 게임만 기록한다
     */
    public record Metrics(
            @DefaultValue("400") @PositiveOrZero int maxPerRun,
            @DefaultValue("30") @Positive int recentDays,
            @DefaultValue("10") @PositiveOrZero int minReviews
    ) {
    }

    public record Scheduler(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("0 0 4 * * *") String cron,
            @DefaultValue("Asia/Seoul") String zone
    ) {
    }
}
