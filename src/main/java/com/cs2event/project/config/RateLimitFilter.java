package com.cs2event.project.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final long CLEANUP_INTERVAL_MS = 120_000L;

    private final boolean enabled;
    private final int apiRequestsPerMinute;
    private final int registrationRequestsPerMinute;
    private final boolean trustForwardedHeaders;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupMs = new AtomicLong();

    public RateLimitFilter(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.api-requests-per-minute:60}") int apiRequestsPerMinute,
            @Value("${app.rate-limit.registration-requests-per-minute:5}") int registrationRequestsPerMinute,
            @Value("${app.rate-limit.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        this.enabled = enabled;
        this.apiRequestsPerMinute = Math.max(1, apiRequestsPerMinute);
        this.registrationRequestsPerMinute = Math.max(1, registrationRequestsPerMinute);
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RateLimitRule rule = ruleFor(request);
        if (!enabled || rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long nowMs = System.currentTimeMillis();
        cleanupExpiredWindows(nowMs);

        String key = rule.name() + ':' + clientIp(request);
        LimitDecision decision = consume(key, rule.requestsPerMinute(), nowMs);
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"Muitas requisicoes. Tente novamente em alguns segundos.\"}");
    }

    private RateLimitRule ruleFor(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        String path = request.getRequestURI();
        if ("POST".equals(method) && "/api/teams".equals(path)) {
            return new RateLimitRule("registration", registrationRequestsPerMinute);
        }
        if (path.equals("/api") || path.startsWith("/api/")) {
            return new RateLimitRule("api", apiRequestsPerMinute);
        }
        return null;
    }

    private LimitDecision consume(String key, int limit, long nowMs) {
        Window window = windows.computeIfAbsent(key, ignored -> new Window(nowMs));
        synchronized (window) {
            long elapsedMs = nowMs - window.startedAtMs;
            if (elapsedMs >= WINDOW_MS) {
                window.startedAtMs = nowMs;
                window.requests = 0;
                elapsedMs = 0;
            }
            if (window.requests >= limit) {
                long retryAfterSeconds = Math.max(1, (WINDOW_MS - elapsedMs + 999) / 1000);
                return new LimitDecision(false, retryAfterSeconds);
            }
            window.requests++;
            return new LimitDecision(true, 0);
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String cfConnectingIp = firstHeaderValue(request.getHeader("CF-Connecting-IP"));
            if (cfConnectingIp != null) {
                return cfConnectingIp;
            }
            String xRealIp = firstHeaderValue(request.getHeader("X-Real-IP"));
            if (xRealIp != null) {
                return xRealIp;
            }
            String xForwardedFor = firstHeaderValue(request.getHeader("X-Forwarded-For"));
            if (xForwardedFor != null) {
                return xForwardedFor;
            }
        }
        return request.getRemoteAddr();
    }

    private String firstHeaderValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String first = value.split(",", 2)[0].trim();
        return first.isEmpty() ? null : first;
    }

    private void cleanupExpiredWindows(long nowMs) {
        long lastCleanup = lastCleanupMs.get();
        if (nowMs - lastCleanup < CLEANUP_INTERVAL_MS
                || !lastCleanupMs.compareAndSet(lastCleanup, nowMs)) {
            return;
        }
        windows.entrySet().removeIf(entry -> nowMs - entry.getValue().startedAtMs > WINDOW_MS * 2);
    }

    private record RateLimitRule(String name, int requestsPerMinute) {
    }

    private record LimitDecision(boolean allowed, long retryAfterSeconds) {
    }

    private static class Window {
        private long startedAtMs;
        private int requests;

        private Window(long startedAtMs) {
            this.startedAtMs = startedAtMs;
        }
    }
}
