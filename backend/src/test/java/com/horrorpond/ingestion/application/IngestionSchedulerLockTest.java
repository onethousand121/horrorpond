package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.support.DatabaseCleaner;
import com.horrorpond.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @SchedulerLock이 프록시로 적용돼, 실행 중에 들어온 두 번째 실행을 건너뛰는지 검증한다.
 * cron "-"으로 자동 실행은 끄고 run()을 직접 호출한다.
 */
@SpringBootTest(properties = {
        "ingestion.scheduler.enabled=true",
        "ingestion.scheduler.cron=-"
})
@Import(TestcontainersConfiguration.class)
class IngestionSchedulerLockTest {

    @Autowired
    IngestionScheduler scheduler;

    @MockitoBean
    IngestionPipeline pipeline;

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
}
