package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.AppDetailsResult;
import com.horrorpond.ingestion.client.IngestionProperties;
import com.horrorpond.ingestion.client.Sleeper;
import com.horrorpond.ingestion.client.SteamRateLimitedException;
import com.horrorpond.ingestion.client.SteamStoreClient;
import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import com.horrorpond.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 예산 = lockAtMostFor(2h) × 0.8 = 96분. 시간 경과는 MutableClock으로 흉내 낸다.
 */
class EnrichmentTimeBudgetTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final long JOB_ID = 1L;

    private final MutableClock clock = new MutableClock(T0);
    private final SteamAppSeedRepository seedRepository = mock(SteamAppSeedRepository.class);
    private final SteamStoreClient storeClient = mock(SteamStoreClient.class);
    private final EnrichmentItemWriter itemWriter = mock(EnrichmentItemWriter.class);
    private final IngestionJobRecorder jobRecorder = mock(IngestionJobRecorder.class);
    private final Sleeper sleeper = clock::advance;

    private final IngestionProperties properties = new IngestionProperties(
            "https://store.test", "https://spy.test", Duration.ofMillis(1500), Duration.ofMinutes(60), 3,
            Duration.ofHours(2),
            new IngestionProperties.Enrichment(2000, Duration.ofDays(7), Duration.ofDays(1), 3),
            new IngestionProperties.Scheduler(false, "-", "UTC"));

    private final EnrichmentService service = new EnrichmentService(
            seedRepository, storeClient, itemWriter, jobRecorder, properties, sleeper, clock);

    @BeforeEach
    void setUp() {
        List<SteamAppSeed> seeds = IntStream.rangeClosed(1, 5)
                .mapToObj(appid -> SteamAppSeed.discovered(appid, DiscoveredBy.STEAMSPY_TAG, T0))
                .toList();
        when(seedRepository.findByFetchStatusAndDiscoveredByOrderByAppidDesc(
                eq(FetchStatus.PENDING), eq(DiscoveredBy.STEAMSPY_TAG), any())).thenReturn(seeds);
        when(jobRecorder.start(JobType.ENRICHMENT, TriggerType.MANUAL)).thenReturn(JOB_ID);
    }

    @Test
    void stopsTakingNewItemsOnceBudgetIsReached() {
        // 아이템마다 40분 소요: 0, 40, 80분에 시작한 3건만 처리하고 120분 시점에 멈춘다
        when(storeClient.fetchAppDetails(anyInt())).thenAnswer(invocation -> {
            clock.advance(Duration.ofMinutes(40));
            return new AppDetailsResult.NotFound();
        });

        service.run(TriggerType.MANUAL);

        verify(storeClient, times(3)).fetchAppDetails(anyInt());
        verify(jobRecorder).succeed(JOB_ID, 3, 0);
        verify(jobRecorder, never()).fail(any(), any());
    }

    @Test
    void accumulatedRateLimitWaitsCountTowardBudget() {
        // 429마다 60분 대기: 1번째 아이템이 429 한 번(60분) + 성공, 2번째도 429 한 번(누적 120분) + 성공
        // → 3번째 시작 전 경과 120분 ≥ 96분이라 중단
        int[] calls = {0};
        doAnswer(invocation -> {
            calls[0]++;
            if (calls[0] % 2 == 1) {
                throw new SteamRateLimitedException(invocation.getArgument(0), 429);
            }
            return new AppDetailsResult.NotFound();
        }).when(storeClient).fetchAppDetails(anyInt());

        service.run(TriggerType.MANUAL);

        verify(storeClient, times(4)).fetchAppDetails(anyInt());
        verify(itemWriter, times(2)).write(anyInt(), any());
        verify(jobRecorder).succeed(JOB_ID, 2, 0);
        assertThat(Duration.between(T0, clock.instant())).isEqualTo(Duration.ofMinutes(120));
    }

    @Test
    void finishesAllItemsWhenWithinBudget() {
        when(storeClient.fetchAppDetails(anyInt())).thenAnswer(invocation -> {
            clock.advance(Duration.ofSeconds(2));
            return new AppDetailsResult.NotFound();
        });

        service.run(TriggerType.MANUAL);

        verify(storeClient, times(5)).fetchAppDetails(anyInt());
        verify(jobRecorder).succeed(JOB_ID, 5, 0);
    }

    @Test
    void budgetIsEightyPercentOfLock() {
        assertThat(properties.runTimeBudget()).isEqualTo(Duration.ofMinutes(96));
    }
}
