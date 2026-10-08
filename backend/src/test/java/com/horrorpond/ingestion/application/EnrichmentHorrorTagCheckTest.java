package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.AppDetailsResult;
import com.horrorpond.ingestion.client.IngestionProperties;
import com.horrorpond.ingestion.client.SteamSpyClient;
import com.horrorpond.ingestion.client.SteamStoreClient;
import com.horrorpond.ingestion.client.SteamTransientException;
import com.horrorpond.ingestion.domain.DiscoveredBy;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.HorrorTag;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SteamSpy 태그 판정 실패는 Steam 호출로 넘어가지 않고, seed 상태도 바꾸지 않는다(다음 실행에서 다시 판정).
 */
class EnrichmentHorrorTagCheckTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final long JOB_ID = 1L;

    private final MutableClock clock = new MutableClock(T0);
    private final SteamAppSeedRepository seedRepository = mock(SteamAppSeedRepository.class);
    private final SteamStoreClient storeClient = mock(SteamStoreClient.class);
    private final SteamSpyClient steamSpyClient = mock(SteamSpyClient.class);
    private final EnrichmentItemWriter itemWriter = mock(EnrichmentItemWriter.class);
    private final IngestionJobRecorder jobRecorder = mock(IngestionJobRecorder.class);

    private final IngestionProperties properties = new IngestionProperties(
            "https://store.test", "https://spy.test", Duration.ofMillis(1500), Duration.ofSeconds(1),
            Duration.ofMinutes(60), 3, Duration.ofHours(2),
            new IngestionProperties.Enrichment(2000, Duration.ofDays(7), Duration.ofDays(1), 3),
            new IngestionProperties.Metrics(0, 30, 10),
            new IngestionProperties.Scheduler(false, "-", "UTC"));

    private final EnrichmentService service = new EnrichmentService(
            seedRepository, storeClient, steamSpyClient, itemWriter, jobRecorder, properties, clock::advance, clock);

    @BeforeEach
    void setUp() {
        when(seedRepository.findByFetchStatusAndDiscoveredByAndHorrorTagNotOrderByAppidDesc(
                eq(FetchStatus.PENDING), eq(DiscoveredBy.STEAMSPY_TAG), eq(HorrorTag.NOT_HORROR), any()))
                .thenReturn(List.of(SteamAppSeed.discovered(10, DiscoveredBy.STEAMSPY_TAG, T0),
                        SteamAppSeed.discovered(20, DiscoveredBy.STEAMSPY_TAG, T0)));
        when(jobRecorder.start(JobType.ENRICHMENT, TriggerType.MANUAL)).thenReturn(JOB_ID);
    }

    @Test
    void tagCheckFailureSkipsItemWithoutTouchingSeed() {
        when(steamSpyClient.fetchTopTags(10)).thenThrow(new SteamTransientException("steamspy down"));
        when(steamSpyClient.fetchTopTags(20)).thenReturn(List.of("Horror"));
        when(storeClient.fetchAppDetails(20)).thenReturn(new AppDetailsResult.NotFound());

        service.run(TriggerType.MANUAL);

        verify(storeClient, never()).fetchAppDetails(10);
        verify(itemWriter, never()).recordSpyTags(eq(10), any(), any());
        verify(itemWriter, never()).markFailed(10);
        verify(itemWriter).recordSpyTags(20, List.of("Horror"), HorrorTag.HORROR);
        verify(jobRecorder).succeed(JOB_ID, 1, 1);
    }

    @Test
    void consecutiveTagCheckFailuresAbortTheJob() {
        when(seedRepository.findByFetchStatusAndDiscoveredByAndHorrorTagNotOrderByAppidDesc(
                eq(FetchStatus.PENDING), eq(DiscoveredBy.STEAMSPY_TAG), eq(HorrorTag.NOT_HORROR), any()))
                .thenReturn(IntStream.rangeClosed(1, 5)
                        .mapToObj(appid -> SteamAppSeed.discovered(appid, DiscoveredBy.STEAMSPY_TAG, T0))
                        .toList());
        when(steamSpyClient.fetchTopTags(anyInt())).thenThrow(new SteamTransientException("steamspy down"));

        service.run(TriggerType.MANUAL);

        verify(steamSpyClient, times(3)).fetchTopTags(anyInt());
        verify(storeClient, never()).fetchAppDetails(anyInt());
        verify(jobRecorder).fail(JOB_ID, "SteamSpy tag check failed 3 times in a row (last appid=3)");
    }

    @Test
    void missingReviewCountIsFilledFromAppReviews() {
        when(steamSpyClient.fetchTopTags(anyInt())).thenReturn(List.of("Horror"));
        when(storeClient.fetchAppDetails(10)).thenReturn(new AppDetailsResult.Found("{\"name\":\"New\"}"));
        when(storeClient.fetchReviewCount(10)).thenReturn(1509);
        when(storeClient.fetchAppDetails(20))
                .thenReturn(new AppDetailsResult.Found("{\"name\":\"Old\",\"recommendations\":{\"total\":7}}"));

        service.run(TriggerType.MANUAL);

        verify(itemWriter).write(10,
                new AppDetailsResult.Found("{\"name\":\"New\",\"recommendations\":{\"total\":1509}}"));
        verify(storeClient, never()).fetchReviewCount(20);
        verify(jobRecorder).succeed(JOB_ID, 2, 0);
    }

    @Test
    void appReviewsFailureStillSavesAppDetails() {
        when(steamSpyClient.fetchTopTags(anyInt())).thenReturn(List.of("Horror"));
        when(storeClient.fetchAppDetails(anyInt())).thenReturn(new AppDetailsResult.Found("{\"name\":\"New\"}"));
        when(storeClient.fetchReviewCount(anyInt())).thenThrow(new SteamTransientException("down"));

        service.run(TriggerType.MANUAL);

        verify(itemWriter).write(10, new AppDetailsResult.Found("{\"name\":\"New\"}"));
        verify(itemWriter).write(20, new AppDetailsResult.Found("{\"name\":\"New\"}"));
        verify(jobRecorder).succeed(JOB_ID, 2, 0);
    }

    @Test
    void manualSeedGetsTagsWithoutHorrorCheckAndTagFailureDoesNotBlock() {
        when(seedRepository.findByFetchStatusAndDiscoveredByAndHorrorTagNotOrderByAppidDesc(
                eq(FetchStatus.PENDING), eq(DiscoveredBy.STEAMSPY_TAG), eq(HorrorTag.NOT_HORROR), any()))
                .thenReturn(List.of());
        when(seedRepository.findByFetchStatusAndDiscoveredByOrderByDiscoveredAtAscAppidAsc(
                eq(FetchStatus.PENDING), eq(DiscoveredBy.MANUAL), any()))
                .thenReturn(List.of(SteamAppSeed.discovered(30, DiscoveredBy.MANUAL, T0),
                        SteamAppSeed.discovered(40, DiscoveredBy.MANUAL, T0)));
        when(steamSpyClient.fetchTopTags(30)).thenReturn(List.of("Battle Royale"));
        when(steamSpyClient.fetchTopTags(40)).thenThrow(new SteamTransientException("steamspy down"));
        when(storeClient.fetchAppDetails(anyInt())).thenReturn(new AppDetailsResult.NotFound());

        service.run(TriggerType.MANUAL);

        // 수동 추가는 판정하지 않으므로 Horror 태그가 없어도 Steam까지 진행한다
        verify(itemWriter).recordSpyTags(30, List.of("Battle Royale"), null);
        verify(itemWriter, never()).recordSpyTags(eq(40), any(), any());
        verify(storeClient).fetchAppDetails(30);
        verify(storeClient).fetchAppDetails(40);
        verify(jobRecorder).succeed(JOB_ID, 2, 0);
    }

    @Test
    void notHorrorIsRecordedAndSkipsSteam() {
        when(steamSpyClient.fetchTopTags(anyInt())).thenReturn(List.of("Battle Royale"));

        service.run(TriggerType.MANUAL);

        verify(itemWriter).recordSpyTags(10, List.of("Battle Royale"), HorrorTag.NOT_HORROR);
        verify(itemWriter).recordSpyTags(20, List.of("Battle Royale"), HorrorTag.NOT_HORROR);
        verify(storeClient, never()).fetchAppDetails(anyInt());
        verify(jobRecorder).succeed(JOB_ID, 2, 0);
    }
}
