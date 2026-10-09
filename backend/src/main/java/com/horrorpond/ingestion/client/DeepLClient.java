package com.horrorpond.ingestion.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepL 번역 API. 인증 헤더는 {@link SteamClientConfig#deepLRestClient}가 붙인다.
 */
@Component
public class DeepLClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int QUOTA_EXCEEDED = 456;
    private static final int FORBIDDEN = 403;
    /** 번역 품질용 맥락. DeepL은 이 글자 수를 과금하지 않는다 */
    static final String CONTEXT = "Short store-page description of a horror video game on Steam.";

    private final RestClient restClient;
    private final boolean configured;

    public DeepLClient(@Qualifier("deepLRestClient") RestClient restClient,
                       @Value("${DEEPL_API_KEY:}") String apiKey) {
        this.restClient = restClient;
        this.configured = apiKey != null && !apiKey.isBlank();
    }

    public boolean isConfigured() {
        return configured;
    }

    /**
     * 이달 사용량. 번역 단계가 시작할 때 남은 한도를 확인한다.
     */
    @Retryable(includes = DeepLTransientException.class, maxRetries = 2, delay = 2000, multiplier = 2)
    public Usage usage() {
        JsonNode body = read(call(restClient.get().uri("/v2/usage"), "usage"), "usage");
        return new Usage(body.path("character_count").asLong(), body.path("character_limit").asLong());
    }

    /**
     * 여러 소개를 한 번에 한국어로 번역한다(원문 언어는 자동 감지). 결과는 요청 순서와 같다.
     */
    @Retryable(includes = DeepLTransientException.class, maxRetries = 2, delay = 5000, multiplier = 2)
    public List<String> translateToKorean(List<String> texts) {
        Map<String, Object> request = Map.of("text", texts, "target_lang", "KO", "context", CONTEXT);
        JsonNode body = read(call(restClient.post().uri("/v2/translate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request), "translate"), "translate");
        JsonNode translations = body.path("translations");
        if (!translations.isArray() || translations.size() != texts.size()) {
            throw new DeepLTransientException("deepl translate returned " + translations.size()
                    + " translations for " + texts.size() + " texts");
        }
        List<String> result = new ArrayList<>(texts.size());
        translations.forEach(item -> result.add(item.path("text").asString()));
        return result;
    }

    private static String call(RestClient.RequestHeadersSpec<?> request, String name) {
        try {
            return request.retrieve()
                    .onStatus(status -> status.value() == QUOTA_EXCEEDED, (req, response) -> {
                        throw new DeepLQuotaExceededException();
                    })
                    .onStatus(status -> status.value() == FORBIDDEN, (req, response) -> {
                        // 키가 틀렸다: 재시도해도 소용없다
                        throw new IllegalStateException("deepl " + name + " rejected the API key (403)");
                    })
                    .onStatus(HttpStatusCode::isError, (req, response) -> {
                        throw new DeepLTransientException(
                                "deepl " + name + " responded " + response.getStatusCode().value());
                    })
                    .body(String.class);
        } catch (ResourceAccessException e) {
            throw new DeepLTransientException("deepl " + name + " I/O failure", e);
        }
    }

    private static JsonNode read(String body, String name) {
        try {
            JsonNode root = body == null ? null : JSON.readTree(body);
            if (root == null || !root.isObject()) {
                throw new DeepLTransientException("deepl " + name + " unexpected body");
            }
            return root;
        } catch (JacksonException e) {
            throw new DeepLTransientException("deepl " + name + " malformed body", e);
        }
    }

    public record Usage(long characterCount, long characterLimit) {

        public long remaining() {
            return Math.max(0, characterLimit - characterCount);
        }
    }
}
