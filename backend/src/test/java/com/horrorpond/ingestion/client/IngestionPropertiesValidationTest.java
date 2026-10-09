package com.horrorpond.ingestion.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 수집 1회 예상 소요 = enrichment(maxPerRun × (2 × requestInterval + steamSpyRequestInterval))
 * + 리뷰 수 기록(metrics.maxPerRun × requestInterval)
 * + itch.io 수집((itch.maxPages + itch.maxPerRun) × itch.requestInterval). 이게 lockAtMostFor의 80%를 넘으면 기동에 실패해야 한다.
 */
class IngestionPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @Test
    void defaultsAreValid() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            IngestionProperties properties = context.getBean(IngestionProperties.class);
            assertThat(properties.enrichment().maxPerRun()).isEqualTo(1200);
            assertThat(properties.lockAtMostFor()).isEqualTo(Duration.ofHours(2));
            // 1200 × (1.5s × 2 + 1s)
            assertThat(properties.estimatedEnrichmentDuration()).isEqualTo(Duration.ofSeconds(4800));
            // 400 × 1.5s, enrichment는 96분에서 이 몫을 뺀 시간까지만 쓴다
            assertThat(properties.metrics().maxPerRun()).isEqualTo(400);
            assertThat(properties.estimatedMetricsDuration()).isEqualTo(Duration.ofSeconds(600));
            // (20 + 100) × 2s
            assertThat(properties.estimatedItchDuration()).isEqualTo(Duration.ofMinutes(4));
            assertThat(properties.enrichmentTimeBudget()).isEqualTo(Duration.ofMinutes(82));
        });
    }

    @Test
    void exactlyEightyPercentIsAllowed() {
        // 1230 × 4s + 400 × 1.5s + 120 × 2s = 82분 + 10분 + 4분 = 96분 = 120분의 80%
        runner.withPropertyValues("ingestion.enrichment.max-per-run=1230")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void disablingMetricsLeavesWholeBudgetToEnrichment() {
        runner.withPropertyValues("ingestion.enrichment.max-per-run=1440", "ingestion.metrics.max-per-run=0",
                        "ingestion.itch.max-pages=0", "ingestion.itch.max-per-run=0")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void tooManyMetricsPerRunFailsStartup() {
        // 1200 × 4s + 481 × 1.5s + 4분 = 80분 + 12분 1.5초 + 4분 > 96분
        runner.withPropertyValues("ingestion.metrics.max-per-run=481")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("80% of lock-at-most-for"));
    }

    @Test
    void tooManyItemsPerRunFailsStartup() {
        runner.withPropertyValues("ingestion.enrichment.max-per-run=1231")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("80% of lock-at-most-for"));
    }

    @Test
    void tooManyItchPagesFailsStartup() {
        // 1200 × 4s + 400 × 1.5s + (20 + 181) × 2s = 80분 + 10분 + 6분 2초 > 96분
        runner.withPropertyValues("ingestion.itch.max-per-run=181")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("80% of lock-at-most-for"));
    }

    @Test
    void shorterLockFailsStartupWithDefaultMaxPerRun() {
        // 1200 × 4s = 80분 > 60분의 80%(48분)
        runner.withPropertyValues("ingestion.lock-at-most-for=1h")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("80% of lock-at-most-for"));
    }

    @Test
    void slowerSteamSpyIntervalFailsStartup() {
        // 1200 × (3s + 2s) = 100분 > 96분
        runner.withPropertyValues("ingestion.steam-spy-request-interval=2s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void slowerIntervalFailsStartup() {
        // 1200 × (6s + 1s) = 140분 > 96분
        runner.withPropertyValues("ingestion.request-interval=3s")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(IngestionProperties.class)
    static class Config {
    }
}
