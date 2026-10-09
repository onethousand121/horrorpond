package com.horrorpond.ingestion.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * itch.io는 공개 목록 API가 없어 사이트가 쓰는 목록 JSON(format=json)과 게임 페이지 HTML을 받는다.
 * Steam과 호출 제한이 따로라 전용 pacer(itch.request-interval)를 쓰고, 재시도마다 pacer를 다시 통과한다.
 */
@Component
public class ItchClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;
    private final SteamRequestPacer pacer;
    private final String baseUrl;

    @Autowired
    public ItchClient(@Qualifier("itchRestClient") RestClient restClient, Clock clock, Sleeper sleeper,
                      IngestionProperties properties) {
        this(restClient, new SteamRequestPacer(clock, sleeper, properties.itch().requestInterval()),
                properties.itch().baseUrl());
    }

    ItchClient(RestClient restClient, SteamRequestPacer pacer, String baseUrl) {
        this.restClient = restClient;
        this.pacer = pacer;
        this.baseUrl = baseUrl;
    }

    /**
     * 공포 태그 평점순 목록 한 페이지(36개). 게임 칸들의 HTML을 돌려주고, 페이지가 끝났으면 빈 문자열.
     */
    @Retryable(includes = ItchTransientException.class, maxRetries = 2, delay = 5000, multiplier = 2)
    public String fetchTopRatedHorrorPage(int page) {
        String body = get(URI.create(baseUrl + "/games/top-rated/tag-horror?format=json&page=" + page),
                "top-rated page " + page);
        if (body == null) {
            return "";
        }
        try {
            JsonNode content = JSON.readTree(body).path("content");
            return content.isString() ? content.asString() : "";
        } catch (JacksonException e) {
            throw new ItchTransientException("top-rated page " + page + " returned malformed JSON", e);
        }
    }

    /**
     * 게임 페이지 HTML. 없는(삭제·비공개) 게임이면 null.
     *
     * @param url {@link ItchUrls#normalize}를 거친 주소
     */
    @Retryable(includes = ItchTransientException.class, maxRetries = 2, delay = 5000, multiplier = 2)
    public String fetchGamePage(String url) {
        return get(URI.create(ItchUrls.normalize(url)), url);
    }

    private String get(URI uri, String label) {
        pacer.acquire();
        try {
            return restClient.get()
                    .uri(uri)
                    .exchange((request, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        if (status.value() == HttpStatus.NOT_FOUND.value()) {
                            return null;
                        }
                        if (status.isError()) {
                            throw new ItchTransientException("itch.io " + label + " responded " + status.value());
                        }
                        return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    });
        } catch (ResourceAccessException e) {
            throw new ItchTransientException("itch.io " + label + " I/O failure", e);
        }
    }
}
