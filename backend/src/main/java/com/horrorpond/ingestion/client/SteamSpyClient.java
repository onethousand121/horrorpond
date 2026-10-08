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
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SteamSpyClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * SteamSpy는 DB가 과부하면 HTTP 200에 "Connection failed: Too many connections" 같은 본문을 준다.
     * 곧바로 다시 부르면 또 실패하므로, 이 응답이면 길게 쉬고 나서 재시도로 넘긴다.
     */
    static final String OVERLOAD_BODY_PREFIX = "Connection failed";
    static final Duration OVERLOAD_BACKOFF = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final SteamRequestPacer pacer;
    private final Sleeper sleeper;

    /**
     * SteamSpy는 Steam Store와 호출 제한이 따로라(appdetails 초당 1회) 전용 pacer를 쓴다.
     */
    @Autowired
    public SteamSpyClient(@Qualifier("steamSpyRestClient") RestClient restClient, Clock clock, Sleeper sleeper,
                          IngestionProperties properties) {
        this(restClient, new SteamRequestPacer(clock, sleeper, properties.steamSpyRequestInterval()), sleeper);
    }

    SteamSpyClient(RestClient restClient, SteamRequestPacer pacer, Sleeper sleeper) {
        this.restClient = restClient;
        this.pacer = pacer;
        this.sleeper = sleeper;
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
     * 과부하 응답이면 {@link #OVERLOAD_BACKOFF}만큼 쉰 뒤 재시도한다(트랜잭션 밖에서 호출된다).
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
        if (body != null && body.stripLeading().startsWith(OVERLOAD_BODY_PREFIX)) {
            sleeper.sleep(OVERLOAD_BACKOFF);
            throw new SteamTransientException("steamspy appdetails appid=" + appid + " overloaded: " + firstLine(body));
        }
        return parseTags(appid, body);
    }

    private static String firstLine(String body) {
        String line = body.strip().lines().findFirst().orElse("");
        return line.length() > 100 ? line.substring(0, 100) : line;
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
