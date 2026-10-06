package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.domain.TriggerType;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 하루 1회 전체 수집. 여러 인스턴스가 떠 있어도 ShedLock으로 한 곳에서만 실행된다.
 * 수동 트리거({@link IngestionTriggerService})와 같은 락 이름을 쓴다.
 */
@Component
@ConditionalOnProperty(prefix = "ingestion.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class IngestionScheduler {

    private final IngestionPipeline pipeline;

    @Scheduled(cron = "${ingestion.scheduler.cron:0 0 4 * * *}", zone = "${ingestion.scheduler.zone:Asia/Seoul}")
    @SchedulerLock(name = IngestionPipeline.LOCK_NAME)
    public void run() {
        pipeline.runSteps(IngestionPipeline.ALL_STEPS, TriggerType.SCHEDULED);
    }
}
