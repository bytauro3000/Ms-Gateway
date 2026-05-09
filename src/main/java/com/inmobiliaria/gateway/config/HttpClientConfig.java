package com.inmobiliaria.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class HttpClientConfig {

    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("render-fixed")
            .maxIdleTime(Duration.ofSeconds(45))
            .maxLifeTime(Duration.ofMinutes(3))
            .pendingAcquireTimeout(Duration.ofSeconds(10))
            .evictInBackground(Duration.ofSeconds(30)) // ← aquí sí funciona via código Java
            .build();
    }

    @Bean
    public HttpClientCustomizer httpClientCustomizer(ConnectionProvider connectionProvider) {
        return httpClient -> HttpClient.create(connectionProvider) // ← inyecta el provider
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(Duration.ofSeconds(160))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(160, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(160, TimeUnit.SECONDS))
            );
    }
}