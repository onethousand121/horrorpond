package com.horrorpond.ingestion.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SteamSpyClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;
    private final SteamRequestPacer pacer;

    /**
     * SteamSpy는 Steam Store와 호출 제한이 따로라(appdetails 초당 1회) 전용 pacer를 쓴다.
     */
    @Autowired
    public SteamSpyClient(@Qualifier("steamSpyRestClient") RestClient restClient, Clock clock, Sleeper sleeper,
                          IngestionProperties properties) {
        this(restClient, new SteamRequestPacer(clock, sleeper, properties.steamSpyRequestInterval()));
    }

    SteamSpyClient(RestClient restClient, SteamRequestPacer pacer) {
        this.restClient = restClient;
        this.pacer = pacer;
    }

    /**
     * 응답은 appid를 key로 하는 JSON object다. 숫자가 아닌 key는 무시한다.
     */
    public Set<Integer> fetchHorrorAppIds() {
        String body = restClient.get()
                .uri(uri -> uri.path("/api.php")
                        .queryParam("request", "tag")
                        .queryParam("tag", "Horror")
                        .build())
                .retrieve()
                .body(String.class);
        Set<Integer> appids = new LinkedHashSet<>();
        if (body == null) {
            return appids;
        }
        for (Map.Entry<String, JsonNode> entry : JSON.readTree(body).properties()) {
            try {
                appids.add(Integer.valueOf(entry.getKey()));
            } catch (NumberFormatException ignored) {
                // appid가 아닌 key
            }
        }
        return appids;
    }

    /**
     * 표가 많은 순서의 상위 태그 이름(최대 20개). tags는 {"Horror": 1234, ...} object이고,
     * 태그가 없거나 SteamSpy가 모르는 appid면 빈 배열([])이 온다. 재시도마다 pacer를 다시 통과한다.
     */
    @Retryable(includes = SteamTransientException.class, maxRetries = 2, delay = 2000, multiplier = 2)
    public List<String> fetchTopTags(int appid) {
        pacer.acquire();
        String body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/api.php")
                            .queryParam("request", "appdetails")
                            .queryParam("appid", appid)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new SteamTransientException(
                                "steamspy appdetails appid=" + appid + " responded " + response.getStatusCode().value());
                    })
                    .body(String.class);
        } catch (ResourceAccessException e) {
            throw new SteamTransientException("steamspy appdetails appid=" + appid + " I/O failure", e);
        }
        return parseTags(appid, body);
    }

    private static List<String> parseTags(int appid, String body) {
        JsonNode root;
        try {
            root = body == null ? null : JSON.readTree(body);
        } catch (JacksonException e) {
            throw new SteamTransientException("steamspy appdetails appid=" + appid + " malformed body", e);
        }
        if (root == null || !root.isObject()) {
            throw new SteamTransientException("steamspy appdetails appid=" + appid + " unexpected body");
        }
        JsonNode tags = root.path("tags");
        List<String> names = new ArrayList<>();
        if (tags.isObject()) {
            tags.properties().forEach(entry -> names.add(entry.getKey()));
        }
        return names;
    }
}
