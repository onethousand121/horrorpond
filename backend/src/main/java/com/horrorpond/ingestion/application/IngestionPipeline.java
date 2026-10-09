package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 수집 단계를 discovery → enrichment → normalize → itch(itch.io) → metrics(리뷰 수 기록) → translate(한국어 자동 번역)
 * 순서로 실행한다. 번역이 마지막이라 그날 들어온 itch.io 게임 소개도 함께 번역된다.
 * 앞 단계가 실패해도 다음 단계는 진행한다 (각 단계는 자기 job에 결과를 남긴다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionPipeline {

    public static final String LOCK_NAME = "steam-ingestion";
    public static final List<JobType> ALL_STEPS = List.of(JobType.DISCOVERY, JobType.ENRICHMENT, JobType.NORMALIZE,
            JobType.ITCH, JobType.METRICS, JobType.TRANSLATE);

    private final DiscoveryService discoveryService;
    private final EnrichmentService enrichmentService;
    private final NormalizeService normalizeService;
    private final ReviewMetricsService reviewMetricsService;
    private final TranslationService translationService;
    private final ItchIngestionService itchIngestionService;

    /**
     * 요청 순서와 무관하게 정해진 단계 순서로, 중복 없이 실행한다.
     */
    public void runSteps(Collection<JobType> steps, TriggerType trigger) {
        for (JobType step : ALL_STEPS) {
            if (!steps.contains(step)) {
                continue;
            }
            try {
                switch (step) {
                    case DISCOVERY -> discoveryService.run(trigger);
                    case ENRICHMENT -> enrichmentService.run(trigger);
                    case NORMALIZE -> normalizeService.run(trigger);
                    case ITCH -> itchIngestionService.run(trigger);
                    case METRICS -> reviewMetricsService.run(trigger);
                    case TRANSLATE -> translationService.run(trigger);
                }
            } catch (RuntimeException e) {
                log.error("Ingestion step {} failed unexpectedly; continuing with next step", step, e);
            }
        }
    }
}
