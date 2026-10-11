package com.horrorpond.ingestion.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 수집 실패 알림 메일 (Resend). API 키는 환경변수 RESEND_API_KEY로만 받는다.
 * 키나 받는 주소(ALERT_EMAIL_TO)가 없으면 메일을 보내지 않고 로그만 남긴다.
 *
 * @param emailTo   받는 주소. 환경변수 ALERT_EMAIL_TO
 * @param emailFrom 보내는 주소. 도메인 인증 전에는 Resend 기본 주소(가입한 계정 메일로만 보낼 수 있다)
 */
@ConfigurationProperties("alert")
public record AlertProperties(
        @DefaultValue("https://api.resend.com") String resendBaseUrl,
        String emailTo,
        @DefaultValue("lurkpond <onboarding@resend.dev>") String emailFrom
) {
}
