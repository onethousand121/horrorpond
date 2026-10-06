package com.horrorpond.ingestion.client;

import com.horrorpond.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SteamSpyClientTest {

    @Test
    void fetchHorrorAppIdsReturnsObjectKeys() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://spy.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SteamSpyClient client = new SteamSpyClient(builder.build());
        server.expect(requestTo("https://spy.test/api.php?request=tag&tag=Horror"))
                .andRespond(withSuccess(Fixtures.steam("steamspy-tag-horror.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchHorrorAppIds()).containsExactlyInAnyOrder(739630, 594330, 1172470);
        server.verify();
    }
}
