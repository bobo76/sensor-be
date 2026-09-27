package com.house.sensors.sensors.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    // Max wait for the response after the request is sent; surfaces as
    // HttpTimeoutException wrapped in ResourceAccessException
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT)
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(RESPONSE_TIMEOUT);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
