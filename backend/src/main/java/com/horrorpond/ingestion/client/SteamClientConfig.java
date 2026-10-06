package com.horrorpond.ingestion.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(IngestionProperties.class)
public class SteamClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient steamStoreRestClient(RestClient.Builder builder, IngestionProperties properties) {
        return builder
                .baseUrl(properties.steamStoreBaseUrl())
                .requestFactory(requestFactory(Duration.ofSeconds(15)))
                .build();
    }

    @Bean
    public RestClient steamSpyRestClient(RestClient.Builder builder, IngestionProperties properties) {
        return builder
                .baseUrl(properties.steamSpyBaseUrl())
                .requestFactory(requestFactory(Duration.ofSeconds(30)))
                .build();
    }

    private static JdkClientHttpRequestFactory requestFactory(Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
