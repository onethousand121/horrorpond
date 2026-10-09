package com.horrorpond.ingestion.client;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 한국어 자동 번역(DeepL). API 키는 환경변수 DEEPL_API_KEY로만 받는다(없으면 번역 단계를 건너뛴다).
 *
 * @param deeplBaseUrl     비우면 키로 정한다: Free 키(끝이 ":fx")는 api-free.deepl.com, 아니면 api.deepl.com
 * @param maxCharsPerRun   한 번에 번역할 최대 글자 수 (DeepL은 원문 글자 수로 센다)
 * @param monthlyReserve   이달 남은 한도 중 남겨 둘 글자 수 (그달 나머지 신작용)
 * @param batchSize        요청 1번에 보내는 소개 수 (DeepL 상한 50)
 * @param recentDays       출시 예정·이 기간 안에 출시한 게임을 먼저 번역한다
 */
@Validated
@ConfigurationProperties("translation")
public record TranslationProperties(
        String deeplBaseUrl,
        @DefaultValue("150000") @Positive int maxCharsPerRun,
        @DefaultValue("20000") @PositiveOrZero int monthlyReserve,
        @DefaultValue("50") @Positive @Max(50) int batchSize,
        @DefaultValue("30") @Positive int recentDays
) {

    static final String FREE_BASE_URL = "https://api-free.deepl.com";
    static final String PRO_BASE_URL = "https://api.deepl.com";

    String baseUrlFor(String apiKey) {
        if (deeplBaseUrl != null && !deeplBaseUrl.isBlank()) {
            return deeplBaseUrl;
        }
        return apiKey != null && apiKey.endsWith(":fx") ? FREE_BASE_URL : PRO_BASE_URL;
    }
}
