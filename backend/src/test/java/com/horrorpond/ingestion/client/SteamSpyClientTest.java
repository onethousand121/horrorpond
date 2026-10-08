package com.horrorpond.ingestion.client;

import com.horrorpond.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SteamSpyClientTest {

    private static final String APPDETAILS_URL = "https://spy.test/api.php?request=appdetails&appid=578080";

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://spy.test");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final List<Duration> sleeps = new ArrayList<>();
    private final SteamSpyClient client = new SteamSpyClient(builder.build(),
            new SteamRequestPacer(Clock.systemUTC(), duration -> { }, Duration.ZERO), sleeps::add);

    @Test
    void fetchHorrorAppIdsReturnsObjectKeys() {
        server.expect(requestTo("https://spy.test/api.php?request=tag&tag=Horror"))
                .andRespond(withSuccess(Fixtures.steam("steamspy-tag-horror.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchHorrorAppIds()).containsExactlyInAnyOrder(739630, 594330, 1172470);
        server.verify();
    }

    @Test
    void fetchTopTagsKeepsVoteOrder() {
        server.expect(requestTo(APPDETAILS_URL)).andRespond(withSuccess(
                "{\"appid\":578080,\"name\":\"PUBG\",\"tags\":{\"Survival\":14893,\"Shooter\":12788,\"Battle Royale\":10936}}",
                MediaType.APPLICATION_JSON));

        assertThat(client.fetchTopTags(578080)).containsExactly("Survival", "Shooter", "Battle Royale");
        server.verify();
    }

    @Test
    void fetchTopTagsReturnsEmptyWhenTagsIsEmptyArray() {
        // SteamSpy는 태그가 없거나 모르는 appid면 tags를 빈 배열로 준다
        server.expect(requestTo(APPDETAILS_URL)).andRespond(withSuccess(
                "{\"appid\":578080,\"name\":null,\"tags\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.fetchTopTags(578080)).isEmpty();
    }

    @Test
    void serverErrorIsTransient() {
        server.expect(requestTo(APPDETAILS_URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.fetchTopTags(578080))
                .isInstanceOf(SteamTransientException.class)
                .hasMessageContaining("responded 502");
    }

    @Test
    void malformedBodyIsTransient() {
        server.expect(requestTo(APPDETAILS_URL)).andRespond(withSuccess("<html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.fetchTopTags(578080)).isInstanceOf(SteamTransientException.class);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void overloadBodyBacksOffBeforeRetry() {
        // 실제로 받은 응답: HTTP 200 + 평문 본문
        server.expect(requestTo(APPDETAILS_URL))
                .andRespond(withSuccess("Connection failed: Too many connections", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.fetchTopTags(578080))
                .isInstanceOf(SteamTransientException.class)
                .hasMessageContaining("overloaded: Connection failed: Too many connections");
        assertThat(sleeps).containsExactly(SteamSpyClient.OVERLOAD_BACKOFF);
    }
}
