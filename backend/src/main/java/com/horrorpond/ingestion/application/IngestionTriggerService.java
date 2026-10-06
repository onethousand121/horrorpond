package com.horrorpond.ingestion.application;

import com.horrorpond.common.domain.DomainStateException;
import com.horrorpond.common.domain.DomainValidationException;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * 수동 수집 트리거. 락은 요청 스레드에서 즉시 잡아 점유 중이면 바로 거절하고,
 * 실행은 전용 단일 스레드에서 비동기로 한다. 락은 실행이 끝난 스레드에서 해제한다.
 * (LockingTaskExecutor는 호출 스레드에서 작업을 끝까지 실행한 뒤에야 락 획득 여부를 알려주므로,
 * 즉시 응답이 필요한 여기서는 같은 LockProvider와 락 이름을 직접 사용한다.)
 */
@Slf4j
@Service
public class IngestionTriggerService implements DisposableBean {

    private final LockProvider lockProvider;
    private final IngestionPipeline pipeline;
    private final Clock clock;
    private final ThreadPoolTaskExecutor executor;

    public IngestionTriggerService(LockProvider lockProvider, IngestionPipeline pipeline, Clock clock) {
        this.lockProvider = lockProvider;
        this.pipeline = pipeline;
        this.clock = clock;
        this.executor = singleThreadExecutor();
    }

    public void trigger(List<JobType> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new DomainValidationException("steps must not be empty");
        }
        LockConfiguration config = new LockConfiguration(
                clock.instant(), IngestionPipeline.LOCK_NAME, IngestionPipeline.LOCK_AT_MOST_FOR, Duration.ZERO);
        SimpleLock lock = lockProvider.lock(config)
                .orElseThrow(() -> new DomainStateException("Ingestion is already running"));
        try {
            executor.execute(() -> {
                try {
                    pipeline.runSteps(steps, TriggerType.MANUAL);
                } finally {
                    lock.unlock();
                }
            });
        } catch (TaskRejectedException e) {
            lock.unlock();
            throw new DomainStateException("Ingestion executor is busy");
        }
        log.info("Manual ingestion triggered: steps={}", steps);
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }

    /**
     * 종료 시 실행 중인 작업을 인터럽트한다. Sleeper가 인터럽트를 받으면 job은 FAILED로 끝나고 락은 해제된다.
     */
    private static ThreadPoolTaskExecutor singleThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("ingestion-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
