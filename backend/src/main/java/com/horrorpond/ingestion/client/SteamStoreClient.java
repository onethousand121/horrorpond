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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SteamStoreClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    /** Steam 스토어의 Horror 태그 id */
    static final int HORROR_TAG_ID = 1667;
    /** 게임만 (DLC, 데모, 사운드트랙 제외) */
    private static final int CATEGORY_GAMES = 998;
    /** 번들은 "1,2,3" 처럼 여러 appid라 숫자 하나인 것만 받는다 */
    private static final Pattern SEARCH_APPID = Pattern.compile("data-ds-appid=\"(\\d+)\"");

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

    /**
     * 전체 리뷰 수 (언어·구매 경로 무관). appdetails에 recommendations가 없을 때만 쓴다.
     *
     * @return 리뷰 수. 응답에 값이 없으면 null
     */
    @Retryable(includes = SteamTransientException.class, maxRetries = 2, delay = 2000, multiplier = 2)
    public Integer fetchReviewCount(int appid) {
        pacer.acquire();
        String body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/appreviews/{appid}")
                            .queryParam("json", 1)
                            .queryParam("language", "all")
                            .queryParam("purchase_type", "all")
                            .queryParam("num_per_page", 0)
                            .build(appid))
                    .retrieve()
                    .onStatus(SteamStoreClient::isRateLimited, (request, response) -> {
                        throw new SteamRateLimitedException(appid, response.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new SteamTransientException(
                                "appreviews appid=" + appid + " responded " + response.getStatusCode().value());
                    })
                    .body(String.class);
        } catch (ResourceAccessException e) {
            throw new SteamTransientException("appreviews appid=" + appid + " I/O failure", e);
        }
        try {
            JsonNode total = body == null ? null : JSON.readTree(body).path("query_summary").get("total_reviews");
            return total != null && total.isIntegralNumber() ? total.asInt() : null;
        } catch (JacksonException e) {
            throw new SteamTransientException("appreviews appid=" + appid + " malformed body", e);
        }
    }

    /**
     * 스토어 검색 결과 한 페이지의 appid (화면 순서). 공식 API가 아니라 검색 페이지가 쓰는 JSON 응답이라
     * results_html에서 data-ds-appid만 뽑는다. 결과가 끝났으면 빈 목록.
     */
    @Retryable(includes = SteamTransientException.class, maxRetries = 2, delay = 2000, multiplier = 2)
    public List<Integer> fetchHorrorSearchPage(SteamSearchList list, int start, int count) {
        pacer.acquire();
        String body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/search/results/")
                            .queryParam("tags", HORROR_TAG_ID)
                            .queryParam("category1", CATEGORY_GAMES)
                            .queryParam(list.paramName, list.paramValue)
                            .queryParam("infinite", 1)
                            .queryParam("start", start)
                            .queryParam("count", count)
                            .queryParam("cc", "kr")
                            .build())
                    .retrieve()
                    .onStatus(SteamStoreClient::isRateLimited, (request, response) -> {
                        throw new SteamRateLimitedException(0, response.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new SteamTransientException(
                                "search " + list + " responded " + response.getStatusCode().value());
                    })
                    .body(String.class);
        } catch (ResourceAccessException e) {
            throw new SteamTransientException("search " + list + " I/O failure", e);
        }
        JsonNode html;
        try {
            html = body == null ? null : JSON.readTree(body).get("results_html");
        } catch (JacksonException e) {
            throw new SteamTransientException("search " + list + " malformed body", e);
        }
        if (html == null || !html.isString()) {
            throw new SteamTransientException("search " + list + " unexpected body");
        }
        List<Integer> appids = new ArrayList<>();
        Matcher matcher = SEARCH_APPID.matcher(html.asString());
        while (matcher.find()) {
            appids.add(Integer.valueOf(matcher.group(1)));
        }
        return appids;
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
