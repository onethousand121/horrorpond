package com.horrorpond.ingestion.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties({IngestionProperties.class, TranslationProperties.class})
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

    /**
     * 키가 없으면 인증 헤더 없이 만든다 (DeepLClient.isConfigured()가 false라 호출하지 않는다).
     */
    @Bean
    public RestClient deepLRestClient(RestClient.Builder builder, TranslationProperties properties,
                                      @Value("${DEEPL_API_KEY:}") String apiKey) {
        return configureDeepL(builder.requestFactory(requestFactory(Duration.ofSeconds(30))), properties, apiKey)
                .build();
    }

    /** 주소와 인증 헤더. 테스트가 같은 설정에 가짜 서버를 붙일 수 있게 나눠 둔다 */
    public static RestClient.Builder configureDeepL(RestClient.Builder builder, TranslationProperties properties,
                                                    String apiKey) {
        builder.baseUrl(properties.baseUrlFor(apiKey));
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "DeepL-Auth-Key " + apiKey);
        }
        return builder;
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
