package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * job 상태 변경을 각각 별도 트랜잭션으로 즉시 커밋해, 실행 중에도 진행 상황이 보이게 한다.
 */
@Component
@RequiredArgsConstructor
public class IngestionJobRecorder {

    private final IngestionJobRepository jobRepository;
    private final Clock clock;

    @Transactional
    public Long start(JobType type, TriggerType trigger) {
        return jobRepository.save(IngestionJob.start(type, trigger, clock.instant())).getId();
    }

    @Transactional
    public void succeed(Long jobId, int processed, int failed) {
        jobRepository.findById(jobId).orElseThrow().succeed(processed, failed, clock.instant());
    }

    @Transactional
    public void fail(Long jobId, String message) {
        jobRepository.findById(jobId).orElseThrow().fail(message, clock.instant());
    }

    static String describe(Throwable e) {
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
