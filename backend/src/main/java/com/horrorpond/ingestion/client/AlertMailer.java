package com.horrorpond.ingestion.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * 관리자에게 알림 메일을 보낸다 (Resend). 인증 헤더는 {@link SteamClientConfig#resendRestClient}가 붙인다.
 * 알림은 부가 기능이라 실패해도 예외를 던지지 않고 로그만 남긴다.
 */
@Slf4j
@Component
public class AlertMailer {

    private final RestClient restClient;
    private final AlertProperties properties;
    private final boolean configured;

    public AlertMailer(@Qualifier("resendRestClient") RestClient restClient, AlertProperties properties,
                       @Value("${RESEND_API_KEY:}") String apiKey) {
        this.restClient = restClient;
        this.properties = properties;
        this.configured = apiKey != null && !apiKey.isBlank()
                && properties.emailTo() != null && !properties.emailTo().isBlank();
    }

    public boolean isConfigured() {
        return configured;
    }

    /**
     * @return 보냈으면 true (설정이 없거나 실패하면 false)
     */
    public boolean send(String subject, String text) {
        if (!configured) {
            log.warn("Alert mail not configured (RESEND_API_KEY / ALERT_EMAIL_TO); would have sent: {}", subject);
            return false;
        }
        try {
            restClient.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", properties.emailFrom(),
                            "to", List.of(properties.emailTo()),
                            "subject", subject,
                            "text", text))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Alert mail sent: {}", subject);
            return true;
        } catch (RestClientException e) {
            log.error("Alert mail failed: {} ({})", subject, e.getMessage());
            return false;
        }
    }
}
