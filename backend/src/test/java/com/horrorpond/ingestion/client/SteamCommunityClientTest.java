package com.horrorpond.ingestion.client;

import com.horrorpond.ingestion.client.SteamCommunityClient.SteamAchievement;
import com.horrorpond.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SteamCommunityClientTest {

    private static final String URL = "https://community.test/stats/4615780/achievements";

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://community.test");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final SteamCommunityClient client = new SteamCommunityClient(builder.build());

    @Test
    void readsAchievementsFromThePageData() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                Fixtures.steam("community-achievements-4615780.html"), MediaType.TEXT_HTML));

        List<SteamAchievement> achievements = client.fetchAchievements(4615780);

        // 보관(archived)된 업적은 빼고, 숨은 업적은 표시와 함께 남긴다
        assertThat(achievements).extracting(SteamAchievement::name)
                .containsExactly("Captivus", "Anxius", "Mortuum", "Hidden One");
        SteamAchievement first = achievements.get(0);
        assertThat(first.description()).isEqualTo("Clear Chapter 1");
        assertThat(first.hidden()).isFalse();
        assertThat(first.iconUrl()).isEqualTo("https://cdn.fastly.steamstatic.com/steamcommunity/public/images/apps/"
                + "4615780/d1d9d84db746d51d7495034e01153ad9eaf40128.jpg");
        SteamAchievement hidden = achievements.get(achievements.size() - 1);
        assertThat(hidden.hidden()).isTrue();
        assertThat(hidden.description()).isNull();
    }

    @Test
    void gameWithoutAchievementPageHasNone() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(client.fetchAchievements(4615780)).isEmpty();
        // 업적 페이지 대신 다른 페이지로 보내진 경우
        assertThat(SteamCommunityClient.parse("<html><body>Steam Community :: Error</body></html>", 1)).isEmpty();
    }

    @Test
    void serverErrorsAndBrokenDataAreTransient() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.fetchAchievements(4615780)).isInstanceOf(SteamTransientException.class);
        assertThatThrownBy(() -> SteamCommunityClient.parse(
                "<script type=\"application/json\" id=\"valve-ssr-data\">{broken</script>", 1))
                .isInstanceOf(SteamTransientException.class);
    }
}
