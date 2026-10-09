package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.IngestionProperties;
import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobStatus;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.support.Utils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * RUNNING에 멈춘 job을 정리한다.
 * <ul>
 *   <li>종료(배포·재시작) 시작 때: 이 인스턴스에서 돌던 job을 바로 FAILED로 바꾸고 이 인스턴스가 잡은 수집 락을 푼다.
 *       (DB가 아직 열려 있는 ContextClosedEvent에서 한다. 작업 스레드는 뒤이어 인터럽트된다)</li>
 *   <li>기동 시: 크래시(강제 종료)로 남은 job을 FAILED로. 기준은 ShedLock lockAtMostFor와 같다
 *       (그보다 오래 실행 중인 job은 없다고 본다).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaleJobCleaner {

    static final String MESSAGE = "interrupted by shutdown";
    static final String SHUTDOWN_MESSAGE = "interrupted by shutdown (deploy or restart); next run continues";

    private final IngestionJobRepository jobRepository;
    private final IngestionJobRecorder jobRecorder;
    private final IngestionProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @EventListener(ContextClosedEvent.class)
    @Transactional
    public void failJobsRunningHereOnShutdown() {
        var running = jobRecorder.runningHere();
        if (running.isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        jobRepository.findAllById(running).stream()
                .filter(IngestionJob::isRunning)
                .forEach(job -> job.fail(SHUTDOWN_MESSAGE, now));
        // ShedLock은 락을 잡은 인스턴스를 호스트 이름으로 기록한다. 이 인스턴스의 락만 바로 푼다
        int released = jdbcTemplate.update(
                // locked_at으로 되돌린다 (ShedLock이 DB 시간을 UTC로 기록하므로 시간대 계산을 하지 않는다)
                "UPDATE shedlock SET lock_until = locked_at WHERE name = ? AND locked_by = ? AND lock_until > locked_at",
                IngestionPipeline.LOCK_NAME, Utils.getHostname());
        log.warn("Shutdown while ingestion was running: marked {} job(s) FAILED, released {} lock(s)",
                running.size(), released);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void failStaleRunningJobs() {
        Instant now = clock.instant();
        List<IngestionJob> stale = jobRepository.findByStatusAndStartedAtBefore(
                JobStatus.RUNNING, now.minus(properties.lockAtMostFor()));
        stale.forEach(job -> job.fail(MESSAGE, now));
        if (!stale.isEmpty()) {
            log.warn("Marked {} stale RUNNING ingestion job(s) as FAILED", stale.size());
        }
    }
}
