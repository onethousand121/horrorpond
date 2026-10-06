package com.horrorpond.ingestion.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * enrichment 1회 예상 소요(maxPerRun × (requestInterval + steamSpyRequestInterval))가 lockAtMostFor의 80%를 넘으면 기동에 실패해야 한다.
 */
class IngestionPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @Test
    void defaultsAreValid() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            IngestionProperties properties = context.getBean(IngestionProperties.class);
            assertThat(properties.enrichment().maxPerRun()).isEqualTo(2000);
            assertThat(properties.lockAtMostFor()).isEqualTo(Duration.ofHours(2));
            // 2000 × (1.5s + 1s)
            assertThat(properties.estimatedEnrichmentDuration()).isEqualTo(Duration.ofSeconds(5000));
        });
    }

    @Test
    void exactlyEightyPercentIsAllowed() {
        // 2304 × 2.5s = 96분 = 120분의 80%
        runner.withPropertyValues("ingestion.enrichment.max-per-run=2304")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void tooManyItemsPerRunFailsStartup() {
        runner.withPropertyValues("ingestion.enrichment.max-per-run=2305")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("80% of lock-at-most-for"));
    }

    @Test
    void shorterLockFailsStartupWithDefaultMaxPerRun() {
        // 2000 × 2.5s ≈ 83분 > 60분의 80%(48분)
        runner.withPropertyValues("ingestion.lock-at-most-for=1h")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("80% of lock-at-most-for"));
    }

    @Test
    void slowerSteamSpyIntervalFailsStartup() {
        // 2000 × (1.5s + 1.5s) = 100분 > 96분
        runner.withPropertyValues("ingestion.steam-spy-request-interval=1500ms")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void slowerIntervalFailsStartup() {
        // 2000 × (3s + 1s) ≈ 133분 > 96분
        runner.withPropertyValues("ingestion.request-interval=3s")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(IngestionProperties.class)
    static class Config {
    }
}
