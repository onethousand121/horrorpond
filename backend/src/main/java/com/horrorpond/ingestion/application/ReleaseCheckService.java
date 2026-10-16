package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.AppDetailsResult;
import com.horrorpond.ingestion.client.IngestionProperties;
import com.horrorpond.ingestion.client.Sleeper;
import com.horrorpond.ingestion.client.SteamRateLimitedException;
import com.horrorpond.ingestion.client.SteamTransientException;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.SteamAppSeed;
import com.horrorpond.ingestion.domain.SteamRawSnapshot;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import com.horrorpond.ingestion.repository.SteamRawSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 매시간 출시 확인. Steam은 출시 날짜만 주고 그날 정해진 시각에 풀리므로, 출시일이 어제~내일인데 아직 "출시 예정"인 게임을
 * 다시 받아 하루 1회 수집을 기다리지 않고 출시·가격·연기를 반영한다.
 * 저장과 반영은 하루 1회 수집과 같은 길(appdetails → 원본 스냅샷 → 정규화)이다.
 * 대상이 없으면 job을 남기지 않고, 실패해도 알림 메일은 보내지 않는다 (알림은 하루 1회 수집만).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseCheckService {

    private static final ZoneId SITE_ZONE = ZoneId.of("Asia/Seoul");
    /** 그 사이에 받은 게임(하루 1회 수집, 직전 확인)은 다시 받지 않는다 */
    static final Duration RECENTLY_FETCHED = Duration.ofMinutes(30);
    /** 429가 이만큼 이어지면 기다리지 않고 이번 실행을 끝낸다 (다음 시간에 다시 한다) */
    static final int MAX_CONSECUTIVE_RATE_LIMITS = 2;

    private final SteamAppSeedRepository seedRepository;
    private final SteamRawSnapshotRepository snapshotRepository;
    private final SteamAppDetailsFetcher appDetailsFetcher;
    private final EnrichmentItemWriter itemWriter;
    private final NormalizeItemProcessor normalizeItemProcessor;
    private final IngestionJobRecorder jobRecorder;
    private final IngestionProperties properties;
    private final Sleeper sleeper;
    private final Clock clock;

    /**
     * @return 만든 job id. 대상이 없으면 null (job을 남기지 않는다)
     */
    public Long run(TriggerType trigger) {
        List<SteamAppSeed> targets = selectTargets();
        if (targets.isEmpty()) {
            log.debug("Release check: no targets");
            return null;
        }
        Long jobId = jobRecorder.start(JobType.RELEASE_CHECK, trigger);
        try {
            Outcome outcome = check(jobId, targets);
            if (outcome.abortReason() != null) {
                jobRecorder.fail(jobId, outcome.abortReason());
            } else {
                jobRecorder.succeed(jobId, outcome.processed(), outcome.failed());
            }
        } catch (RuntimeException e) {
            log.error("Release check failed", e);
            jobRecorder.fail(jobId, IngestionJobRecorder.describe(e));
        }
        return jobId;
    }

    /** 출시일이 어제~내일(한국 날짜)인 "출시 예정" 게임 (숨김 제외), 출시일 순 */
    List<SteamAppSeed> selectTargets() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, SITE_ZONE);
        return seedRepository.findReleaseWindowRefreshTargets(today.minusDays(1), today.plusDays(1),
                now.minus(RECENTLY_FETCHED), properties.releaseCheck().maxPerRun());
    }

    private Outcome check(Long jobId, List<SteamAppSeed> seeds) {
        Instant startedAt = clock.instant();
        Duration budget = properties.releaseCheck().runTimeBudget();
        int processed = 0;
        int failed = 0;
        int consecutiveRateLimits = 0;
        for (int i = 0; i < seeds.size(); i++) {
            if (Duration.between(startedAt, clock.instant()).compareTo(budget) >= 0) {
                log.warn("Release check time budget reached, processed {} / remaining {}", i, seeds.size() - i);
                break;
            }
            jobRecorder.progress(jobId, processed, failed);
            int appid = seeds.get(i).getAppid();
            AppDetailsResult result;
            try {
                result = appDetailsFetcher.fetch(appid);
                consecutiveRateLimits = 0;
            } catch (SteamRateLimitedException e) {
                consecutiveRateLimits++;
                if (consecutiveRateLimits >= MAX_CONSECUTIVE_RATE_LIMITS) {
                    String reason = "Rate limited " + consecutiveRateLimits
                            + " times in a row (last appid=" + appid + ", status=" + e.getStatusCode() + ")";
                    log.warn("Release check aborted: {}", reason);
                    return new Outcome(processed, failed, reason);
                }
                log.warn("Rate limited (appid={}, status={}), waiting {}", appid, e.getStatusCode(),
                        properties.rateLimitWait());
                sleeper.sleep(properties.rateLimitWait());
                i--; // 같은 게임을 다시 받는다
                continue;
            } catch (SteamTransientException e) {
                // seed 상태는 그대로 두고(실패 횟수도 올리지 않는다) 다음 시간에 다시 받는다
                consecutiveRateLimits = 0;
                log.warn("Release check appdetails failed: appid={}, {}", appid, e.getMessage());
                failed++;
                continue;
            }
            try {
                itemWriter.write(appid, result);
                if (needsNormalize(appid)) {
                    normalizeItemProcessor.process(appid);
                }
                processed++;
            } catch (RuntimeException e) {
                // 정규화를 못 했으면 스냅샷이 남아 다음 정규화(하루 1회 수집)가 반영한다
                log.warn("Release check save failed: appid={}, {}", appid, IngestionJobRecorder.describe(e));
                failed++;
            }
        }
        log.info("Release check finished: targets={}, processed={}, failed={}", seeds.size(), processed, failed);
        return new Outcome(processed, failed, null);
    }

    private boolean needsNormalize(int appid) {
        return snapshotRepository.findById(appid).map(SteamRawSnapshot::needsNormalize).orElse(false);
    }

    private record Outcome(int processed, int failed, String abortReason) {
    }
}
