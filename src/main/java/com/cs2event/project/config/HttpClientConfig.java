package com.cs2event.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient asaasRestClient(
            @Value("${asaas.api-url}") String apiUrl,
            @Value("${asaas.api-key}") String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("ASAAS_API_KEY nao configurada");
        }
        return RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("access_token", apiKey.trim())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "CS2Tournament/1.0.0")
                .build();
    }
}
