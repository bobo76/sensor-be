package com.house.sensors.sensors.config;

import io.netty.channel.ChannelOption;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {
    private static final int CONNECTION_TIMEOUT_MS = 5000; // 5 seconds
    // Max wait for the response after the request is sent; surfaces as
    // ReadTimeoutException wrapped in WebClientRequestException
    private static final int RESPONSE_TIMEOUT_SECONDS = 10; // 10 seconds
    private static final int MAX_CONNECTIONS = 50;

    @Bean
    public WebClient webClient() {
        // Configure connection pool
        ConnectionProvider connectionProvider = ConnectionProvider.builder("arduino-pool")
                .maxConnections(MAX_CONNECTIONS)
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .maxIdleTime(Duration.ofSeconds(20))
                .build();

        final HttpClient httpClient = HttpClient.create(connectionProvider)
                .resolver(DefaultAddressResolverGroup.INSTANCE)  // Use JVM DNS resolver
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
