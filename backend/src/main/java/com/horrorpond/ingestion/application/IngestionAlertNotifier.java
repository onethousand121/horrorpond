package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.AlertMailer;
import com.horrorpond.ingestion.domain.IngestionJob;
import com.horrorpond.ingestion.domain.JobStatus;
import com.horrorpond.ingestion.domain.TriggerType;
import com.horrorpond.ingestion.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 수집 한 번이 끝나면, 실패한 단계가 있을 때만 관리자에게 메일 한 통을 보낸다.
 */
@Component
@RequiredArgsConstructor
public class IngestionAlertNotifier {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(KST);

    private final IngestionJobRepository jobRepository;
    private final AlertMailer mailer;

    /**
     * @param jobIds     이번 실행에서 만든 job
     * @param unexpected job 밖에서 터진 오류 ("단계: 메시지")
     * @return 메일을 보냈으면 true
     */
    public boolean notifyIfFailed(TriggerType trigger, Collection<Long> jobIds, List<String> unexpected) {
        List<IngestionJob> failed = jobRepository.findAllById(jobIds).stream()
                .filter(job -> job.getStatus() == JobStatus.FAILED)
                .sorted(Comparator.comparing(IngestionJob::getId))
                .toList();
        if (failed.isEmpty() && unexpected.isEmpty()) {
            return false;
        }
        String steps = failed.stream().map(job -> job.getType().name()).collect(Collectors.joining(", "));
        String subject = "[lurkpond] 수집 실패: " + (steps.isEmpty() ? "예상치 못한 오류" : steps);
        StringBuilder text = new StringBuilder()
                .append(trigger == TriggerType.SCHEDULED ? "매일 04:00 수집" : "수동 수집")
                .append("에서 실패한 단계가 있습니다.\n\n");
        for (IngestionJob job : failed) {
            text.append("■ ").append(job.getType()).append(" (job #").append(job.getId()).append(")\n")
                    .append("  시작 ").append(format(job.getStartedAt()))
                    .append(" / 종료 ").append(format(job.getFinishedAt())).append(" (KST)\n")
                    .append("  처리 ").append(job.getProcessedCount())
                    .append(" / 실패 ").append(job.getFailedCount()).append('\n')
                    .append("  오류: ").append(job.getErrorMessage() == null ? "-" : job.getErrorMessage())
                    .append("\n\n");
        }
        for (String error : unexpected) {
            text.append("■ ").append(error).append("\n\n");
        }
        text.append("자세한 내용은 관리 API의 /api/admin/ingestion/jobs 나 서버 로그에서 확인하세요.\n");
        return mailer.send(subject, text.toString());
    }

    /**
     * 설정(RESEND_API_KEY·ALERT_EMAIL_TO)이 맞는지 관리 화면에서 확인하는 테스트 메일.
     *
     * @return 보냈으면 true (설정이 없거나 Resend가 거절하면 false)
     */
    public boolean sendTest() {
        return mailer.send("[lurkpond] 알림 메일 테스트",
                "수집 실패 알림 메일이 제대로 도착하는지 확인하는 테스트입니다.\n"
                        + "이 메일이 보이면 설정이 맞습니다. 실제 알림은 매일 수집에서 실패한 단계가 있을 때만 옵니다.\n");
    }

    private static String format(Instant instant) {
        return instant == null ? "-" : TIME.format(instant);
    }
}
