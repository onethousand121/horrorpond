package com.horrorpond.ingestion.application;

import com.horrorpond.catalog.domain.GameMetricDaily;
import com.horrorpond.catalog.repository.GameRepository;
import com.horrorpond.catalog.repository.GameRepository.TranslationTarget;
import com.horrorpond.ingestion.client.DeepLClient;
import com.horrorpond.ingestion.client.DeepLQuotaExceededException;
import com.horrorpond.ingestion.client.DeepLTransientException;
import com.horrorpond.ingestion.client.TranslationProperties;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 한국어 소개가 없는 게임의 소개를 DeepL로 한국어로 번역한다. 키(DEEPL_API_KEY)가 없으면 건너뛴다.
 * 이달 남은 한도(무료 50만 자)에서 monthlyReserve를 남기고, 한 번에 maxCharsPerRun까지만 쓴다.
 * 못 한 게임은 다음 실행(다음 날·다음 달)에 이어서 한다.
 * HTTP 호출은 트랜잭션 밖에서 하고, 저장은 {@link TranslationWriter}가 1건씩 커밋한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

    /** 연속으로 이만큼 배치가 실패하면 DeepL 장애로 보고 멈춘다 */
    static final int MAX_CONSECUTIVE_FAILURES = 3;
    /** 한도와 상관없이 한 번에 살펴볼 최대 게임 수 */
    static final int MAX_CANDIDATES = 5000;

    private final GameRepository gameRepository;
    private final DeepLClient deepLClient;
    private final TranslationWriter writer;
    private final IngestionJobRecorder jobRecorder;
    private final TranslationProperties properties;
    private final Clock clock;

    /**
     * @return job id. 키가 없어 건너뛰면 null
     */
    public Long run(TriggerType trigger) {
        if (!deepLClient.isConfigured()) {
            log.info("DEEPL_API_KEY is not set; skipping translation");
            return null;
        }
        Long jobId = jobRecorder.start(JobType.TRANSLATE, trigger);
        try {
            long budget = Math.min(properties.maxCharsPerRun(),
                    deepLClient.usage().remaining() - properties.monthlyReserve());
            Outcome outcome = budget <= 0 ? new Outcome(0, 0, null) : translate(budget);
            if (outcome.abortReason() != null) {
                jobRecorder.fail(jobId, outcome.abortReason());
            } else {
                jobRecorder.succeed(jobId, outcome.processed(), outcome.failed());
            }
        } catch (RuntimeException e) {
            log.error("Translation failed", e);
            jobRecorder.fail(jobId, IngestionJobRecorder.describe(e));
        }
        return jobId;
    }

    private Outcome translate(long budget) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), GameMetricDaily.ZONE);
        List<TranslationTarget> candidates = gameRepository.findKoreanTranslationTargets(
                today.minusDays(properties.recentDays()), MAX_CANDIDATES);
        int processed = 0;
        int failed = 0;
        int consecutiveFailures = 0;
        long used = 0;
        List<TranslationTarget> batch = new ArrayList<>();
        int batchChars = 0;
        for (int i = 0; i <= candidates.size(); i++) {
            TranslationTarget next = i < candidates.size() ? candidates.get(i) : null;
            boolean fits = next != null && used + batchChars + next.getShortDescription().length() <= budget;
            if (fits && batch.size() < properties.batchSize()) {
                batch.add(next);
                batchChars += next.getShortDescription().length();
                continue;
            }
            if (!batch.isEmpty()) {
                try {
                    processed += send(batch);
                    used += batchChars;
                    consecutiveFailures = 0;
                } catch (DeepLQuotaExceededException e) {
                    log.info("DeepL quota exceeded; continuing next month (translated {} so far)", processed);
                    return new Outcome(processed, failed, null);
                } catch (DeepLTransientException e) {
                    log.warn("DeepL translate failed after retries: {}", e.getMessage());
                    failed += batch.size();
                    if (++consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        return new Outcome(processed, failed,
                                "DeepL failed " + consecutiveFailures + " times in a row: " + e.getMessage());
                    }
                }
                batch = new ArrayList<>();
                batchChars = 0;
            }
            if (!fits) {
                break; // 한도 소진 (또는 후보 끝)
            }
            batch.add(next);
            batchChars += next.getShortDescription().length();
        }
        log.info("Translation finished: candidates={}, translated={}, failed={}, chars={}",
                candidates.size(), processed, failed, used);
        return new Outcome(processed, failed, null);
    }

    private int send(List<TranslationTarget> batch) {
        List<String> sources = batch.stream().map(TranslationTarget::getShortDescription).toList();
        List<String> translated = deepLClient.translateToKorean(sources);
        int applied = 0;
        for (int i = 0; i < batch.size(); i++) {
            if (writer.apply(batch.get(i).getGameId(), sources.get(i), translated.get(i))) {
                applied++;
            }
        }
        return applied;
    }

    private record Outcome(int processed, int failed, String abortReason) {
    }
}
