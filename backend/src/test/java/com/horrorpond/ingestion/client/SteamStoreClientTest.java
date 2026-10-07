package com.horrorpond.ingestion.client;

import com.horrorpond.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @Retryable이 실제 프록시를 거쳐 동작하는지 보기 위해 최소 Spring 컨텍스트에서 검증한다.
 */
@SpringJUnitConfig(SteamStoreClientTest.Config.class)
class SteamStoreClientTest {

    private static final String BASE = "https://store.test";
    private static final int APPID = 739630;
    private static final String URL = BASE + "/api/appdetails?appids=" + APPID + "&cc=kr&l=koreana";

    @Autowired
    SteamStoreClient client;

    @Autowired
    MockRestServiceServer server;

    @BeforeEach
    void resetServer() {
        server.reset();
    }

    @Test
    void foundReturnsOnlyDataNode() {
        server.expect(once(), requestTo(URL))
                .andRespond(withSuccess(Fixtures.appDetails(APPID), MediaType.APPLICATION_JSON));

        AppDetailsResult result = client.fetchAppDetails(APPID);

        assertThat(result).isInstanceOf(AppDetailsResult.Found.class);
        JsonNode data = JsonMapper.builder().build().readTree(((AppDetailsResult.Found) result).dataJson());
        assertThat(data.get("steam_appid").asInt()).isEqualTo(APPID);
        assertThat(data.get("name").asString()).isEqualTo("Phasmophobia");
        assertThat(data.has("success")).isFalse();
        server.verify();
    }

    @Test
    void searchPageReturnsSingleAppidsInOrderAndSkipsBundles() {
        server.expect(once(), requestTo(BASE + "/search/results/?tags=1667&category1=998&sort_by=Released_DESC"
                        + "&infinite=1&start=100&count=100&cc=kr"))
                .andRespond(withSuccess("""
                        {"success":1,"results_html":"<a data-ds-appid=\\"30\\">a</a>\
                        <a data-ds-bundleid=\\"9\\" data-ds-appid=\\"1,2\\">b</a><a data-ds-appid=\\"10\\">c</a>",
                        "total_count":3}""", MediaType.APPLICATION_JSON));

        assertThat(client.fetchHorrorSearchPage(SteamSearchList.NEW_RELEASES, 100, 100)).containsExactly(30, 10);
        server.verify();
    }

    @Test
    void searchWithoutResultsHtmlIsTransient() {
        server.expect(times(3), requestTo(BASE + "/search/results/?tags=1667&category1=998"
                        + "&filter=popularcomingsoon&infinite=1&start=0&count=100&cc=kr"))
                .andRespond(withSuccess("{\"success\":2}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchHorrorSearchPage(SteamSearchList.POPULAR_UPCOMING, 0, 100))
                .isInstanceOf(SteamTransientException.class);
        server.verify();
    }

    @Test
    void reviewCountReadsTotalReviews() {
        server.expect(once(), requestTo(BASE + "/appreviews/" + APPID
                        + "?json=1&language=all&purchase_type=all&num_per_page=0"))
                .andRespond(withSuccess("""
                        {"success":1,"query_summary":{"num_reviews":0,"total_positive":2287,
                        "total_negative":296,"total_reviews":2583},"reviews":[]}""", MediaType.APPLICATION_JSON));

        assertThat(client.fetchReviewCount(APPID)).isEqualTo(2583);
        server.verify();
    }

    @Test
    void reviewCountIsNullWhenSummaryMissing() {
        server.expect(once(), requestTo(BASE + "/appreviews/" + APPID
                        + "?json=1&language=all&purchase_type=all&num_per_page=0"))
                .andRespond(withSuccess("{\"success\":2}", MediaType.APPLICATION_JSON));

        assertThat(client.fetchReviewCount(APPID)).isNull();
    }

    @Test
    void successFalseIsNotFound() {
        server.expect(once(), requestTo(BASE + "/api/appdetails?appids=1&cc=kr&l=koreana"))
                .andRespond(withSuccess(Fixtures.appDetails(1), MediaType.APPLICATION_JSON));

        assertThat(client.fetchAppDetails(1)).isInstanceOf(AppDetailsResult.NotFound.class);
        server.verify();
    }

    @Test
    void retriesTwiceOn5xxThenSucceedsThroughProxy() {
        assertThat(AopUtils.isAopProxy(client)).isTrue();
        server.expect(times(2), requestTo(URL)).andRespond(withServerError());
        server.expect(once(), requestTo(URL))
                .andRespond(withSuccess(Fixtures.appDetails(APPID), MediaType.APPLICATION_JSON));

        long start = System.nanoTime();
        AppDetailsResult result = client.fetchAppDetails(APPID);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(result).isInstanceOf(AppDetailsResult.Found.class);
        server.verify();
        // backoff 2s + 4s
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(5900));
    }

    @Test
    void ioFailuresExhaustRetriesAfterThreeCalls() {
        server.expect(times(3), requestTo(URL)).andRespond(withException(new IOException("connection reset")));

        assertThatThrownBy(() -> client.fetchAppDetails(APPID))
                .isInstanceOf(SteamTransientException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 403})
    void rateLimitIsNotRetried(int status) {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status)));

        assertThatThrownBy(() -> client.fetchAppDetails(APPID))
                .isInstanceOfSatisfying(SteamRateLimitedException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(status));
        server.verify();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableResilientMethods
    @Import(SteamStoreClient.class)
    static class Config {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder().baseUrl(BASE);
        }

        @Bean
        MockRestServiceServer mockServer(RestClient.Builder restClientBuilder) {
            return MockRestServiceServer.bindTo(restClientBuilder).build();
        }

        @Bean
        RestClient steamStoreRestClient(RestClient.Builder restClientBuilder, MockRestServiceServer mockServer) {
            return restClientBuilder.build();
        }

        @Bean
        SteamRequestPacer steamRequestPacer() {
            return new SteamRequestPacer(Clock.systemUTC(), duration -> { }, Duration.ZERO);
        }
    }
}
