package com.horrorpond.ingestion.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Steam 커뮤니티 업적 페이지(https://steamcommunity.com/stats/{appid}/achievements)에서 업적 목록을 읽는다.
 * 관리 화면에서 업적 공략을 채울 때만 한 번씩 부르는 기능이라 수집 파이프라인·Steam pacer와 무관하다.
 * 키가 필요한 Web API 대신, 페이지가 화면을 그리려고 심어 둔 JSON(script#valve-ssr-data →
 * renderContext.queryData → 쿼리 ["achievements", appid, "schema_v2"])을 쓴다. 이름·설명은 영어다.
 */
@Component
public class SteamCommunityClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern SSR_DATA = Pattern.compile(
            "<script[^>]*\\bid=\"valve-ssr-data\"[^>]*>(.*?)</script>", Pattern.DOTALL);
    /**
     * 업적 아이콘이 있는 곳. 최근 게임의 아이콘은 이 주소에만 있다(예전 cdn.*.steamstatic.com/steamcommunity/... 는 404).
     * 페이지 설정(Config.MEDIA_CDN_COMMUNITY_URL)이 같은 모양이면 그 값을 쓴다
     */
    private static final String DEFAULT_COMMUNITY_CDN = "https://shared.fastly.steamstatic.com/community_assets/";
    private static final Pattern COMMUNITY_CDN = Pattern.compile(
            "https://shared\\.[a-z]+\\.steamstatic\\.com/community_assets/");
    private static final Pattern ICON_FILE = Pattern.compile("[0-9a-f]{40}\\.(jpg|png)");

    private final RestClient restClient;

    public SteamCommunityClient(@Qualifier("steamCommunityRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @return 업적 목록(게임에 정한 순서). 업적이 없거나 없는 게임이면 빈 목록
     * @throws SteamTransientException 응답 오류·통신 실패·페이지 모양이 바뀌어 읽지 못함
     */
    public List<SteamAchievement> fetchAchievements(int appid) {
        String html;
        try {
            html = restClient.get()
                    .uri("/stats/{appid}/achievements", appid)
                    .exchange((request, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        if (status.value() == HttpStatus.NOT_FOUND.value()) {
                            return "";
                        }
                        if (status.isError()) {
                            throw new SteamTransientException(
                                    "Steam community achievements " + appid + " responded " + status.value());
                        }
                        return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    });
        } catch (ResourceAccessException e) {
            throw new SteamTransientException("Steam community achievements " + appid + " I/O failure", e);
        }
        return parse(html == null ? "" : html, appid);
    }

    static List<SteamAchievement> parse(String html, int appid) {
        Matcher matcher = SSR_DATA.matcher(html);
        if (!matcher.find()) {
            // 업적 페이지가 없는 게임은 다른 페이지(오류·홈)로 보내진다
            return List.of();
        }
        try {
            JsonNode ssr = JSON.readTree(matcher.group(1));
            String cdn = ssr.path("Config").path("MEDIA_CDN_COMMUNITY_URL").asString("");
            if (!COMMUNITY_CDN.matcher(cdn).matches()) {
                cdn = DEFAULT_COMMUNITY_CDN;
            }
            JsonNode queryData = ssr.path("renderContext").path("queryData");
            if (!queryData.isString()) {
                return List.of();
            }
            for (JsonNode query : JSON.readTree(queryData.asString()).path("queries")) {
                JsonNode key = query.path("queryKey");
                if ("achievements".equals(key.path(0).asString("")) && key.path(1).asInt(-1) == appid
                        && "schema_v2".equals(key.path(2).asString(""))) {
                    return achievements(query.path("state").path("data"), appid, cdn);
                }
            }
            return List.of();
        } catch (JacksonException e) {
            throw new SteamTransientException("Steam community achievements " + appid + " returned malformed data", e);
        }
    }

    private static List<SteamAchievement> achievements(JsonNode data, int appid, String cdn) {
        List<SteamAchievement> result = new ArrayList<>();
        for (JsonNode group : data.path("groups")) {
            for (JsonNode achievement : group.path("achievements")) {
                String name = achievement.path("name").asString("").strip();
                if (name.isEmpty() || achievement.path("archived").asBoolean(false)) {
                    continue;
                }
                String icon = achievement.path("icon_achieved").asString("");
                String iconUrl = ICON_FILE.matcher(icon).matches()
                        ? cdn + "images/apps/" + appid + "/" + icon
                        : null;
                String description = achievement.path("description").asString("").strip();
                result.add(new SteamAchievement(name, description.isEmpty() ? null : description,
                        achievement.path("hidden").asBoolean(false), iconUrl));
            }
        }
        return result;
    }

    /**
     * @param description 없으면 null (숨은 업적은 비어 있기도 하다)
     * @param hidden      Steam에서 숨긴 업적(달성 전에는 설명이 안 보임)
     * @param iconUrl     달성 아이콘. 없으면 null
     */
    public record SteamAchievement(String name, String description, boolean hidden, String iconUrl) {
    }
}
