package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.AppDetailsResult;
import com.horrorpond.ingestion.client.IngestionProperties;
import com.horrorpond.ingestion.client.Sleeper;
import com.horrorpond.ingestion.client.SteamRateLimitedException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * seed별 appdetails를 받아 원본 스냅샷으로 저장한다.
 * SteamSpy로 발견한 seed는 먼저 SteamSpy 상위 태그로 공포게임인지 판정하고, 아니면 Steam 호출 없이 제외한다.
 * HTTP 호출과 대기는 트랜잭션 밖에서 하고, 저장은 {@link EnrichmentItemWriter}가 1건씩 커밋한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichmentService {

    /** SteamSpy 장애 시 아이템마다 재시도를 반복하며 예산을 낭비하지 않도록, 연속 실패가 이만큼이면 중단한다. */
    static final int MAX_CONSECUTIVE_TAG_CHECK_FAILURES = 3;

    private final SteamAppSeedRepository seedRepository;
    private final SteamStoreClient storeClient;
    private final SteamSpyClient steamSpyClient;
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
     * 우선순위 (공포게임이 아니라고 판정된 NOT_HORROR는 모두 제외):
     * 1) 수동 추가한 PENDING (관리자가 명시적으로 원한 것)
     * 2) SteamSpy로 발견한 PENDING, appid 내림차순 (최신 게임 먼저)
     * 3) 갱신 주기가 지난 OK (오래된 순)
     * 4) 재시도 대기가 지난 FAILED (실패 횟수 제한 이내)
     */
    List<SteamAppSeed> selectTargets() {
        IngestionProperties.Enrichment config = properties.enrichment();
        Instant now = clock.instant();
        Map<Integer, SteamAppSeed> targets = new LinkedHashMap<>();
        addAll(targets, config.maxPerRun(), remaining -> seedRepository
                .findByFetchStatusAndDiscoveredByOrderByDiscoveredAtAscAppidAsc(
                        FetchStatus.PENDING, DiscoveredBy.MANUAL, remaining));
        addAll(targets, config.maxPerRun(), remaining -> seedRepository
                .findByFetchStatusAndDiscoveredByAndHorrorTagNotOrderByAppidDesc(
                        FetchStatus.PENDING, DiscoveredBy.STEAMSPY_TAG, HorrorTag.NOT_HORROR, remaining));
        addAll(targets, config.maxPerRun(), remaining -> seedRepository
                .findByFetchStatusAndHorrorTagNotAndLastFetchedAtBeforeOrderByLastFetchedAtAscAppidAsc(
                        FetchStatus.OK, HorrorTag.NOT_HORROR, now.minus(config.refreshAfter()), remaining));
        addAll(targets, config.maxPerRun(), remaining -> seedRepository
                .findByFetchStatusAndHorrorTagNotAndFailCountLessThanAndLastFetchedAtBeforeOrderByLastFetchedAtAscAppidAsc(
                        FetchStatus.FAILED, HorrorTag.NOT_HORROR, config.maxFailCount(),
                        now.minus(config.failedRetryAfter()), remaining));
        return new ArrayList<>(targets.values());
    }

    private static void addAll(Map<Integer, SteamAppSeed> targets, int max,
                               Function<Limit, List<SteamAppSeed>> query) {
        int remaining = max - targets.size();
        if (remaining <= 0) {
            return;
        }
        query.apply(Limit.of(remaining)).forEach(seed -> targets.putIfAbsent(seed.getAppid(), seed));
    }

    /**
     * 새 아이템을 시작하기 전마다 경과 시간을 확인한다. 429 대기가 누적돼도 락 유지 시간 안에서 끝내기 위해,
     * 예산(lockAtMostFor × 0.8)을 넘으면 남은 아이템은 다음 실행으로 넘기고 정상 종료한다.
     */
    private Outcome enrich(List<SteamAppSeed> seeds) {
        Instant startedAt = clock.instant();
        Duration budget = properties.runTimeBudget();
        int processed = 0;
        int failed = 0;
        int excluded = 0;
        int consecutiveRateLimits = 0;
        int consecutiveTagCheckFailures = 0;
        for (int i = 0; i < seeds.size(); i++) {
            if (Duration.between(startedAt, clock.instant()).compareTo(budget) >= 0) {
                log.warn("Enrichment time budget reached, processed {} / remaining {}", i, seeds.size() - i);
                break;
            }
            SteamAppSeed seed = seeds.get(i);
            int appid = seed.getAppid();
            // SteamSpy 태그: 모든 seed가 한 번 받는다(장르 자동 분류). SteamSpy로 발견한 seed는 이 태그로 공포 판정도 한다
            boolean checkHorror = seed.needsHorrorTagCheck();
            if (checkHorror || seed.needsSpyTags()) {
                List<String> tags;
                try {
                    tags = steamSpyClient.fetchTopTags(appid);
                } catch (SteamTransientException e) {
                    log.warn("SteamSpy tags failed after retries: appid={}, {}", appid, e.getMessage());
                    if (!checkHorror) {
                        tags = null; // 수동 추가 seed는 태그 없이 진행하고 다음 갱신 때 다시 받는다
                    } else {
                        // 판정 실패는 Steam 실패 횟수에 넣지 않는다. UNCHECKED로 남아 다음 실행에서 다시 판정한다.
                        failed++;
                        consecutiveTagCheckFailures++;
                        if (consecutiveTagCheckFailures >= MAX_CONSECUTIVE_TAG_CHECK_FAILURES) {
                            String reason = "SteamSpy tag check failed " + consecutiveTagCheckFailures
                                    + " times in a row (last appid=" + appid + ")";
                            log.warn("Enrichment aborted: {}", reason);
                            return new Outcome(processed, failed, reason);
                        }
                        continue;
                    }
                }
                if (tags != null) {
                    consecutiveTagCheckFailures = 0;
                    HorrorTag horrorTag = checkHorror ? HorrorTag.classify(tags) : null;
                    itemWriter.recordSpyTags(appid, tags, horrorTag);
                    if (horrorTag == HorrorTag.NOT_HORROR) {
                        excluded++;
                        processed++;
                        continue;
                    }
                }
            }
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
        log.info("Enrichment finished: targets={}, processed={} (excluded as not horror={}), failed={}",
                seeds.size(), processed, excluded, failed);
        return new Outcome(processed, failed, null);
    }

    private record Outcome(int processed, int failed, String abortReason) {
    }
}
