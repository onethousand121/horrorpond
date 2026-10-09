package com.horrorpond.ingestion.domain;

import com.horrorpond.common.domain.DomainStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(name = "ingestion_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class IngestionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    @Column(nullable = false)
    private int processedCount;

    @Column(nullable = false)
    private int failedCount;

    @Column(columnDefinition = "text")
    private String errorMessage;

    public static IngestionJob start(JobType type, TriggerType trigger, Instant now) {
        IngestionJob job = new IngestionJob();
        job.type = Objects.requireNonNull(type, "type");
        job.triggerType = Objects.requireNonNull(trigger, "trigger");
        job.status = JobStatus.RUNNING;
        job.startedAt = Objects.requireNonNull(now, "now");
        return job;
    }

    public boolean isRunning() {
        return status == JobStatus.RUNNING;
    }

    /**
     * 실행 중 진행 상황. 끝날 때 succeed가 최종 값으로 덮어쓴다.
     */
    public void progress(int processed, int failed) {
        if (status != JobStatus.RUNNING) {
            throw new DomainStateException("Job is already finished: status=" + status);
        }
        this.processedCount = processed;
        this.failedCount = failed;
    }

    public void succeed(int processed, int failed, Instant now) {
        finish(JobStatus.SUCCEEDED, now);
        this.processedCount = processed;
        this.failedCount = failed;
    }

    public void fail(String message, Instant now) {
        finish(JobStatus.FAILED, now);
        this.errorMessage = message;
    }

    private void finish(JobStatus result, Instant now) {
        if (status != JobStatus.RUNNING) {
            throw new DomainStateException("Job is already finished: status=" + status);
        }
        this.status = result;
        this.finishedAt = Objects.requireNonNull(now, "now");
    }
}
