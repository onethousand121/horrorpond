package com.horrorpond.ingestion.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
public class SteamSpyClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;

    public SteamSpyClient(@Qualifier("steamSpyRestClient") RestClient restClient) {
        this.restClient = restClient;
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
}
