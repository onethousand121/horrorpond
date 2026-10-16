package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.domain.TriggerType;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매시간 출시 확인 ({@link ReleaseCheckService}). 하루 1회 수집·수동 실행과 같은 락 이름을 써서,
 * 그쪽이 돌고 있으면 이번 시간은 건너뛰고 함께 돌지 않는다. 락 유지 시간은 이 실행에 맞게 짧다.
 */
@Component
@ConditionalOnProperty(prefix = "ingestion.release-check", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ReleaseCheckScheduler {

    private final ReleaseCheckService releaseCheckService;

    @Scheduled(cron = "${ingestion.release-check.cron:0 20 * * * *}",
            zone = "${ingestion.release-check.zone:Asia/Seoul}")
    @SchedulerLock(name = IngestionPipeline.LOCK_NAME,
            lockAtMostFor = "${ingestion.release-check.lock-at-most-for:PT15M}")
    public void run() {
        releaseCheckService.run(TriggerType.SCHEDULED);
    }
}
