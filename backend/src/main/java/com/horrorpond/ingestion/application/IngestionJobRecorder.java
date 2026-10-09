package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * job 상태 변경을 각각 별도 트랜잭션으로 즉시 커밋해, 실행 중에도 진행 상황이 보이게 한다.
 * 이 인스턴스에서 실행 중인 job을 기억해, 종료(배포·재시작) 때 {@link StaleJobCleaner}가 바로 정리하게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionJobRecorder {

    /** 실행 중 진행 상황을 이 건수마다 기록한다 */
    static final int PROGRESS_EVERY = 50;

    private final IngestionJobRepository jobRepository;
    private final Clock clock;
    private final Set<Long> runningHere = ConcurrentHashMap.newKeySet();

    @Transactional
    public Long start(JobType type, TriggerType trigger) {
        Long id = jobRepository.save(IngestionJob.start(type, trigger, clock.instant())).getId();
        runningHere.add(id);
        return id;
    }

    /**
     * done(처리+실패)이 {@link #PROGRESS_EVERY}의 배수일 때만 기록한다. 아이템마다 불러도 된다.
     */
    @Transactional
    public void progress(Long jobId, int processed, int failed) {
        int done = processed + failed;
        if (done == 0 || done % PROGRESS_EVERY != 0) {
            return;
        }
        jobRepository.findById(jobId).filter(IngestionJob::isRunning)
                .ifPresent(job -> job.progress(processed, failed));
    }

    @Transactional
    public void succeed(Long jobId, int processed, int failed) {
        runningHere.remove(jobId);
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        if (job.isRunning()) {
            job.succeed(processed, failed, clock.instant());
        } else {
            log.warn("Job {} was already closed ({}), result processed={} failed={} not recorded",
                    jobId, job.getStatus(), processed, failed);
        }
    }

    @Transactional
    public void fail(Long jobId, String message) {
        runningHere.remove(jobId);
        IngestionJob job = jobRepository.findById(jobId).orElseThrow();
        if (job.isRunning()) {
            job.fail(message, clock.instant());
        }
    }

    /** 이 인스턴스에서 아직 끝나지 않은 job (종료 시 정리용) */
    Set<Long> runningHere() {
        return Set.copyOf(runningHere);
    }

    static String describe(Throwable e) {
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
