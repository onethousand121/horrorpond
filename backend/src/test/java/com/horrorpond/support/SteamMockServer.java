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

    public static String steamSpyHorrorUrl() {
        return SPY_BASE + "/api.php?request=tag&tag=Horror";
    }
}
