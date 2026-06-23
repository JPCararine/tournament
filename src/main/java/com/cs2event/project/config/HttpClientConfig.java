package com.cs2event.project.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/** Beans dos clientes HTTP externos (AbacatePay + SendGrid). */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient abacatePayRestClient(
            @Value("${abacatepay.api-url}") String apiUrl,
            @Value("${abacatepay.token}") String token) {
        return RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    @Bean
    public SendGrid sendGrid(@Value("${sendgrid.api-key}") String apiKey) {
        return new SendGrid(apiKey);
    }
}
