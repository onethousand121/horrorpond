package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobStatus;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 종료/크래시로 RUNNING에 멈춘 job을 기동 시 FAILED로 정리한다.
 * 기준 2시간은 ShedLock lockAtMostFor와 같다 (그보다 오래 실행 중인 job은 없다고 본다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaleJobCleaner {

    static final Duration STALE_AFTER = Duration.ofHours(2);
    static final String MESSAGE = "interrupted by shutdown";

    private final IngestionJobRepository jobRepository;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void failStaleRunningJobs() {
        Instant now = clock.instant();
        List<IngestionJob> stale = jobRepository.findByStatusAndStartedAtBefore(JobStatus.RUNNING, now.minus(STALE_AFTER));
        stale.forEach(job -> job.fail(MESSAGE, now));
        if (!stale.isEmpty()) {
            log.warn("Marked {} stale RUNNING ingestion job(s) as FAILED", stale.size());
        }
    }
}
