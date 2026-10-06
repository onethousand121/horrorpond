package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.AppDetailsResult;
import com.horrorpond.ingestion.client.IngestionProperties;
import com.horrorpond.ingestion.client.Sleeper;
import com.horrorpond.ingestion.client.SteamRateLimitedException;
import com.horrorpond.ingestion.client.SteamStoreClient;
import com.horrorpond.ingestion.client.SteamTransientException;
import com.horrorpond.ingestion.domain.FetchStatus;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * seed별 appdetails를 받아 원본 스냅샷으로 저장한다.
 * HTTP 호출과 대기는 트랜잭션 밖에서 하고, 저장은 {@link EnrichmentItemWriter}가 1건씩 커밋한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichmentService {

    private final SteamAppSeedRepository seedRepository;
    private final SteamStoreClient storeClient;
    private final EnrichmentItemWriter itemWriter;
    private final IngestionJobRecorder jobRecorder;
    private final IngestionProperties properties;
    private final Sleeper sleeper;
    private final Clock clock;

    public Long run(TriggerType trigger) {
        Long jobId = jobRecorder.start(JobType.ENRICHMENT, trigger);
        try {
            Outcome outcome = enrich(selectTargets());
            if (outcome.abortReason() != null) {
                jobRecorder.fail(jobId, outcome.abortReason());
            } else {
                jobRecorder.succeed(jobId, outcome.processed(), outcome.failed());
            }
        } catch (RuntimeException e) {
            log.error("Enrichment failed", e);
            jobRecorder.fail(jobId, IngestionJobRecorder.describe(e));
        }
        return jobId;
    }

    /**
     * 우선순위: PENDING → 갱신 주기가 지난 OK → 재시도 대기가 지난 FAILED(실패 횟수 제한 이내).
     */
    List<Integer> selectTargets() {
        IngestionProperties.Enrichment config = properties.enrichment();
        Instant now = clock.instant();
        Set<Integer> targets = new LinkedHashSet<>();
        addAll(targets, config.maxPerRun(), remaining -> seedRepository
                .findByFetchStatusOrderByDiscoveredAtAscAppidAsc(FetchStatus.PENDING, remaining));
        addAll(targets, config.maxPerRun(), remaining -> seedRepository
                .findByFetchStatusAndLastFetchedAtBeforeOrderByLastFetchedAtAscAppidAsc(
                        FetchStatus.OK, now.minus(config.refreshAfter()), remaining));
        addAll(targets, config.maxPerRun(), remaining -> seedRepository
                .findByFetchStatusAndFailCountLessThanAndLastFetchedAtBeforeOrderByLastFetchedAtAscAppidAsc(
                        FetchStatus.FAILED, config.maxFailCount(), now.minus(config.failedRetryAfter()), remaining));
        return new ArrayList<>(targets);
    }

    private static void addAll(Set<Integer> targets, int max,
                               Function<Limit, List<SteamAppSeed>> query) {
        int remaining = max - targets.size();
        if (remaining <= 0) {
            return;
        }
        query.apply(Limit.of(remaining)).forEach(seed -> targets.add(seed.getAppid()));
    }

    private Outcome enrich(List<Integer> appids) {
        int processed = 0;
        int failed = 0;
        int consecutiveRateLimits = 0;
        for (int appid : appids) {
            while (true) {
                try {
                    AppDetailsResult result = storeClient.fetchAppDetails(appid);
                    consecutiveRateLimits = 0;
                    itemWriter.write(appid, result);
                    processed++;
                    break;
                } catch (SteamRateLimitedException e) {
                    consecutiveRateLimits++;
                    if (consecutiveRateLimits >= properties.maxConsecutiveRateLimits()) {
                        String reason = "Rate limited " + consecutiveRateLimits
                                + " times in a row (last appid=" + appid + ", status=" + e.getStatusCode() + ")";
                        log.warn("Enrichment aborted: {}", reason);
                        return new Outcome(processed, failed, reason);
                    }
                    log.warn("Rate limited (appid={}, status={}), waiting {}", appid, e.getStatusCode(),
                            properties.rateLimitWait());
                    sleeper.sleep(properties.rateLimitWait());
                } catch (SteamTransientException e) {
                    consecutiveRateLimits = 0;
                    log.warn("appdetails failed after retries: appid={}, {}", appid, e.getMessage());
                    itemWriter.markFailed(appid);
                    failed++;
                    break;
                }
            }
        }
        log.info("Enrichment finished: targets={}, processed={}, failed={}", appids.size(), processed, failed);
        return new Outcome(processed, failed, null);
    }

    private record Outcome(int processed, int failed, String abortReason) {
    }
}
