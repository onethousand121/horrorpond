package com.horrorpond.ingestion.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SteamStoreClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;
    private final SteamRequestPacer pacer;

    public SteamStoreClient(@Qualifier("steamStoreRestClient") RestClient restClient, SteamRequestPacer pacer) {
        this.restClient = restClient;
        this.pacer = pacer;
    }

    /**
     * 재시도(@Retryable)마다 pacer를 다시 통과하므로 재시도도 고정 간격을 지킨다.
     */
    @Retryable(includes = SteamTransientException.class, maxRetries = 2, delay = 2000, multiplier = 2)
    public AppDetailsResult fetchAppDetails(int appid) {
        pacer.acquire();
        String body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/api/appdetails")
                            .queryParam("appids", appid)
                            .queryParam("cc", "kr")
                            .queryParam("l", "koreana")
                            .build())
                    .retrieve()
                    .onStatus(SteamStoreClient::isRateLimited, (request, response) -> {
                        throw new SteamRateLimitedException(appid, response.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new SteamTransientException(
                                "appdetails appid=" + appid + " responded " + response.getStatusCode().value());
                    })
                    .body(String.class);
        } catch (ResourceAccessException e) {
            throw new SteamTransientException("appdetails appid=" + appid + " I/O failure", e);
        }
        return parse(appid, body);
    }

    private static boolean isRateLimited(HttpStatusCode status) {
        return status.value() == 429 || status.value() == 403;
    }

    private static AppDetailsResult parse(int appid, String body) {
        JsonNode entry;
        try {
            entry = body == null ? null : JSON.readTree(body).get(String.valueOf(appid));
        } catch (JacksonException e) {
            throw new SteamTransientException("appdetails appid=" + appid + " malformed body", e);
        }
        if (entry == null || !entry.isObject()) {
            throw new SteamTransientException("appdetails appid=" + appid + " unexpected body");
        }
        JsonNode data = entry.get("data");
        if (!entry.path("success").asBoolean(false) || data == null || !data.isObject()) {
            return new AppDetailsResult.NotFound();
        }
        return new AppDetailsResult.Found(data.toString());
    }
}
