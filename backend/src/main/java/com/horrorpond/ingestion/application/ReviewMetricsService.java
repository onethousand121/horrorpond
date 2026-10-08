package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.GameMetricDaily;
import com.horrorpond.catalog.repository.GameMetricDailyRepository;
import com.horrorpond.catalog.repository.GameMetricDailyRepository.SteamMetricTarget;
import com.horrorpond.ingestion.client.IngestionProperties;
import com.horrorpond.ingestion.client.Sleeper;
import com.horrorpond.ingestion.client.SteamRateLimitedException;
import com.horrorpond.ingestion.client.SteamStoreClient;
import com.horrorpond.ingestion.client.SteamTransientException;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * 하루 1회 Steam 리뷰 수 기록(트렌드용). 대상은 {@link GameMetricDailyRepository#findSteamMetricTargets} 참고.
 * 게임마다 appreviews 1번만 부르므로 상세 갱신(enrichment, 7일 주기)보다 훨씬 자주 리뷰 수를 갱신할 수 있다.
 * HTTP 호출과 대기는 트랜잭션 밖에서 하고, 저장은 {@link ReviewMetricsWriter}가 1건씩 커밋한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewMetricsService {

    private final GameMetricDailyRepository metricRepository;
    private final SteamStoreClient storeClient;
    private final ReviewMetricsWriter writer;
    private final IngestionJobRecorder jobRecorder;
    private final IngestionProperties properties;
    private final Sleeper sleeper;
    private final Clock clock;

    public Long run(TriggerType trigger) {
        Long jobId = jobRecorder.start(JobType.METRICS, trigger);
        try {
            Outcome outcome = record(selectTargets());
            if (outcome.abortReason() != null) {
                jobRecorder.fail(jobId, outcome.abortReason());
            } else {
                jobRecorder.succeed(jobId, outcome.processed(), outcome.failed());
            }
        } catch (RuntimeException e) {
            log.error("Review metrics failed", e);
            jobRecorder.fail(jobId, IngestionJobRecorder.describe(e));
        }
        return jobId;
    }

    List<SteamMetricTarget> selectTargets() {
        IngestionProperties.Metrics config = properties.metrics();
        if (config.maxPerRun() == 0) {
            return List.of();
        }
        LocalDate today = today();
        return metricRepository.findSteamMetricTargets(today, today.minusDays(config.recentDays()),
                config.minReviews(), config.maxPerRun());
    }

    private Outcome record(List<SteamMetricTarget> targets) {
        LocalDate today = today();
        int processed = 0;
        int failed = 0;
        int consecutiveRateLimits = 0;
        for (SteamMetricTarget target : targets) {
            int appid = Integer.parseInt(target.getAppid());
            while (true) {
                try {
                    Integer reviewCount = storeClient.fetchReviewCount(appid);
                    consecutiveRateLimits = 0;
                    if (reviewCount == null) {
                        // Steam이 리뷰 정보를 안 주는 게임(지역 제한 등). 다음 실행에서 다시 본다
                        failed++;
                    } else {
                        writer.record(target.getGameId(), reviewCount, today);
                        processed++;
                    }
                    break;
                } catch (SteamRateLimitedException e) {
                    consecutiveRateLimits++;
                    if (consecutiveRateLimits >= properties.maxConsecutiveRateLimits()) {
                        String reason = "Rate limited " + consecutiveRateLimits
                                + " times in a row (last appid=" + appid + ", status=" + e.getStatusCode() + ")";
                        log.warn("Review metrics aborted: {}", reason);
                        return new Outcome(processed, failed, reason);
                    }
                    log.warn("Rate limited (appid={}, status={}), waiting {}", appid, e.getStatusCode(),
                            properties.rateLimitWait());
                    sleeper.sleep(properties.rateLimitWait());
                } catch (SteamTransientException e) {
                    consecutiveRateLimits = 0;
                    log.warn("appreviews failed after retries: appid={}, {}", appid, e.getMessage());
                    failed++;
                    break;
                }
            }
        }
        log.info("Review metrics finished: targets={}, processed={}, failed={}", targets.size(), processed, failed);
        return new Outcome(processed, failed, null);
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), GameMetricDaily.ZONE);
    }

    private record Outcome(int processed, int failed, String abortReason) {
    }
}
