package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.AlertMailer;
import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobType;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngestionAlertNotifierTest {

    private static final Instant T0 = Instant.parse("2026-10-11T19:00:00Z"); // 04:00 KST

    private final IngestionJobRepository jobRepository = mock(IngestionJobRepository.class);
    private final AlertMailer mailer = mock(AlertMailer.class);
    private final IngestionAlertNotifier notifier = new IngestionAlertNotifier(jobRepository, mailer);

    @Test
    void testMailReportsWhetherItWasSent() {
        when(mailer.send(any(), any())).thenReturn(true);
        assertThat(notifier.sendTest()).isTrue();
        verify(mailer).send(org.mockito.ArgumentMatchers.eq("[lurkpond] 알림 메일 테스트"), any());

        when(mailer.send(any(), any())).thenReturn(false);
        assertThat(notifier.sendTest()).isFalse();
    }

    @Test
    void sendsNothingWhenEveryStepSucceeded() {
        when(jobRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                job(1L, JobType.DISCOVERY, null), job(2L, JobType.ITCH, null)));

        assertThat(notifier.notifyIfFailed(TriggerType.SCHEDULED, List.of(1L, 2L), List.of())).isFalse();
        verify(mailer, never()).send(any(), any());
    }

    @Test
    void sendsOneMailListingFailedSteps() {
        when(jobRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(
                job(3L, JobType.TRANSLATE, "DeepL failed 3 times in a row"),
                job(1L, JobType.DISCOVERY, null),
                job(2L, JobType.ITCH, "page has 36 game cells but none could be read")));
        when(mailer.send(any(), any())).thenReturn(true);

        assertThat(notifier.notifyIfFailed(TriggerType.SCHEDULED, List.of(1L, 2L, 3L),
                List.of("METRICS: IllegalStateException: boom"))).isTrue();

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(subject.capture(), text.capture());
        assertThat(subject.getValue()).isEqualTo("[lurkpond] 수집 실패: ITCH, TRANSLATE");
        assertThat(text.getValue())
                .contains("매일 04:00 수집")
                .contains("■ ITCH (job #2)", "시작 10-12 04:00:00", "none could be read")
                .contains("■ TRANSLATE (job #3)", "DeepL failed")
                .contains("■ METRICS: IllegalStateException: boom")
                .doesNotContain("DISCOVERY");
    }

    @Test
    void unexpectedErrorAloneStillAlerts() {
        when(jobRepository.findAllById(List.of())).thenReturn(List.of());

        notifier.notifyIfFailed(TriggerType.MANUAL, List.of(), List.of("ENRICHMENT: NullPointerException: x"));

        verify(mailer).send(org.mockito.ArgumentMatchers.eq("[lurkpond] 수집 실패: 예상치 못한 오류"), any());
    }

    private static IngestionJob job(long id, JobType type, String error) {
        IngestionJob job = IngestionJob.start(type, TriggerType.SCHEDULED, T0);
        ReflectionTestUtils.setField(job, "id", id);
        if (error == null) {
            job.succeed(10, 0, T0.plusSeconds(60));
        } else {
            job.fail(error, T0.plusSeconds(60));
        }
        return job;
    }
}
