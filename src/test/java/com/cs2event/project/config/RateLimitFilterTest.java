package com.cs2event.project.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void blocksRegistrationRequestsAfterConfiguredLimitForSameIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 60, 2, false);
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (request, response) -> chainCalls.incrementAndGet();

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(registrationRequest("203.0.113.10"), first, chain);

        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(registrationRequest("203.0.113.10"), second, chain);

        MockHttpServletResponse third = new MockHttpServletResponse();
        filter.doFilter(registrationRequest("203.0.113.10"), third, chain);

        assertThat(chainCalls).hasValue(2);
        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader("Retry-After")).isNotBlank();
    }

    @Test
    void doesNotRateLimitWebhookEndpoint() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 1, 1, false);
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (request, response) -> chainCalls.incrementAndGet();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/webhooks/asaas");
        request.setRemoteAddr("203.0.113.10");

        filter.doFilter(request, new MockHttpServletResponse(), chain);
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chainCalls).hasValue(2);
    }

    private MockHttpServletRequest registrationRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/teams");
        request.setRemoteAddr(ip);
        return request;
    }
}
