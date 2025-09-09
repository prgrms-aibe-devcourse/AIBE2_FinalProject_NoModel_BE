package com.example.nomodel._core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /** remove.bg 용 WebClient — BG_PROVIDER=removebg 일 때만 생성 */
    @Bean(name = "removeBgWebClient")
    @ConditionalOnProperty(name = "BG_PROVIDER", havingValue = "removebg")
    public WebClient removeBgWebClient(
            @Value("${REMOVEBG_API_BASE_URL}") String baseUrl,
            @Value("${REMOVEBG_API_KEY}") String apiKey,
            @Value("${BG_TIMEOUT_SEC:60}") long timeoutSec
    ) {
        HttpClient http = HttpClient.create().responseTimeout(Duration.ofSeconds(timeoutSec));
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .defaultHeader("X-Api-Key", apiKey)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                        .build())
                .build();
    }

    /** Gemini 용 WebClient — BG_PROVIDER=gemini 일 때만 생성 */
    @Bean(name = "geminiWebClient")
    @ConditionalOnProperty(name = "BG_PROVIDER", havingValue = "gemini")
    public WebClient geminiWebClient(
            @Value("${GEMINI_API_BASE_URL}") String baseUrl,
            @Value("${GEMINI_API_KEY}") String apiKey,
            @Value("${BG_TIMEOUT_SEC:60}") long timeoutSec
    ) {
        HttpClient http = HttpClient.create().responseTimeout(Duration.ofSeconds(timeoutSec));
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                        .build())
                .build();
    }

}