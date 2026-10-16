package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.support.DatabaseCleaner;
import com.horrorpond.support.TestcontainersConfiguration;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @SchedulerLock이 프록시로 적용돼, 실행 중에 들어온 두 번째 실행을 건너뛰는지 검증한다 (매시간 출시 확인 포함).
 * cron "-"으로 자동 실행은 끄고 run()을 직접 호출한다.
 */
@SpringBootTest(properties = {
        "ingestion.scheduler.enabled=true",
        "ingestion.scheduler.cron=-",
        "ingestion.release-check.enabled=true",
        "ingestion.release-check.cron=-"
})
@Import(TestcontainersConfiguration.class)
class IngestionSchedulerLockTest {

    @Autowired
    IngestionScheduler scheduler;

    @MockitoBean
    IngestionPipeline pipeline;

    @Autowired
    ReleaseCheckScheduler releaseCheckScheduler;

    @Autowired
    LockProvider lockProvider;

    @MockitoBean
    ReleaseCheckService releaseCheckService;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clean() {
        DatabaseCleaner.clean(jdbc);
    }

    @Test
    void secondRunIsSkippedWhileFirstHoldsTheLock() throws Exception {
        assertThat(AopUtils.isAopProxy(scheduler)).as("@SchedulerLock 프록시 적용").isTrue();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        doAnswer(invocation -> {
            firstStarted.countDown();
            assertThat(releaseFirst.await(10, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(pipeline).runSteps(any(), eq(TriggerType.SCHEDULED));

        CompletableFuture<Void> first = CompletableFuture.runAsync(scheduler::run);
        assertThat(firstStarted.await(10, TimeUnit.SECONDS)).isTrue();
        // usingDbTime은 lock_until(timestamp)을 UTC 기준으로 저장한다
        assertThat(jdbc.queryForObject("select count(*) from shedlock where name = 'steam-ingestion'"
                + " and lock_until > timezone('utc', now())", Integer.class)).isEqualTo(1);

        scheduler.run();
        verify(pipeline, times(1)).runSteps(any(), any());

        releaseFirst.countDown();
        first.get(10, TimeUnit.SECONDS);

        scheduler.run();
        verify(pipeline, times(2)).runSteps(any(), any());
    }

    /**
     * 매시간 출시 확인은 하루 1회 수집과 같은 락을 쓴다: 수집(또는 수동 실행)이 락을 잡고 있으면 건너뛰고,
     * 자기가 돌 때는 짧은 락 유지 시간으로 잡는다.
     */
    @Test
    void releaseCheckIsSkippedWhileDailyPipelineHoldsTheLock() {
        assertThat(AopUtils.isAopProxy(releaseCheckScheduler)).as("@SchedulerLock 프록시 적용").isTrue();
        SimpleLock pipelineLock = lockProvider.lock(new LockConfiguration(Instant.now(), IngestionPipeline.LOCK_NAME,
                Duration.ofHours(2), Duration.ZERO)).orElseThrow();

        releaseCheckScheduler.run();
        verify(releaseCheckService, never()).run(any());

        pipelineLock.unlock();
        AtomicReference<Long> lockMinutes = new AtomicReference<>();
        doAnswer(invocation -> {
            // usingDbTime은 lock_until(timestamp)을 UTC 기준으로 저장한다
            lockMinutes.set(jdbc.queryForObject("select extract(epoch from lock_until - timezone('utc', now()))"
                    + " / 60 from shedlock where name = ?", Long.class, IngestionPipeline.LOCK_NAME));
            return null;
        }).when(releaseCheckService).run(TriggerType.SCHEDULED);

        releaseCheckScheduler.run();
        verify(releaseCheckService, times(1)).run(TriggerType.SCHEDULED);
        assertThat(lockMinutes.get()).as("하루 1회 수집(2시간)보다 짧은 락").isBetween(1L, 15L);
    }
}
