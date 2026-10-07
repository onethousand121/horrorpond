package com.horrorpond.ingestion.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * enrichment 1회 예상 소요(maxPerRun × (2 × requestInterval + steamSpyRequestInterval))가 lockAtMostFor의 80%를 넘으면 기동에 실패해야 한다.
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
        });
    }

    @Test
    void exactlyEightyPercentIsAllowed() {
        // 1440 × 4s = 96분 = 120분의 80%
        runner.withPropertyValues("ingestion.enrichment.max-per-run=1440")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void tooManyItemsPerRunFailsStartup() {
        runner.withPropertyValues("ingestion.enrichment.max-per-run=1441")
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
