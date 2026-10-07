package com.horrorpond.support;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Steam Store/SteamSpy RestClient를 하나의 MockRestServiceServer에 묶는다.
 * 통합 테스트에서 @TestBean으로 실제 RestClient 빈을 교체할 때 쓴다.
 */
public final class SteamMockServer {

    public static final String STORE_BASE = "https://store.test";
    public static final String SPY_BASE = "https://spy.test";

    private static final RestClient.Builder BUILDER = RestClient.builder();

    public static final MockRestServiceServer SERVER = MockRestServiceServer.bindTo(BUILDER)
            .ignoreExpectOrder(true)
            .build();

    private SteamMockServer() {
    }

    public static RestClient storeRestClient() {
        return BUILDER.clone().baseUrl(STORE_BASE).build();
    }

    public static RestClient spyRestClient() {
        return BUILDER.clone().baseUrl(SPY_BASE).build();
    }

    public static String appDetailsUrl(int appid) {
        return STORE_BASE + "/api/appdetails?appids=" + appid + "&cc=kr&l=koreana";
    }

    public static String appDetailsEnglishUrl(int appid) {
        return STORE_BASE + "/api/appdetails?appids=" + appid + "&l=english&filters=basic,release_date";
    }

    /**
     * @param listParam "sort_by=Released_DESC" 또는 "filter=popularcomingsoon"
     */
    public static String steamSearchUrl(String listParam, int start) {
        return STORE_BASE + "/search/results/?tags=1667&category1=998&" + listParam + "&infinite=1&start=" + start
                + "&count=100&cc=kr";
    }

    public static String steamSearchBody(int... appids) {
        StringBuilder html = new StringBuilder();
        for (int appid : appids) {
            html.append("<a href=\\\"https://store.steampowered.com/app/").append(appid)
                    .append("\\\" data-ds-appid=\\\"").append(appid).append("\\\">x</a>");
        }
        return "{\"success\":1,\"results_html\":\"" + html + "\",\"total_count\":" + appids.length + "}";
    }

    public static String steamSpyAppDetailsUrl(int appid) {
        return SPY_BASE + "/api.php?request=appdetails&appid=" + appid;
    }

    public static String steamSpyHorrorUrl() {
        return SPY_BASE + "/api.php?request=tag&tag=Horror";
    }
}
