package com.bookshelf.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter implements Filter {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limit-cleanup");
        t.setDaemon(true);
        return t;
    });

    private final int maxTokens;
    private final long cleanupIntervalMinutes;

    public RateLimitFilter(
            @Value("${rate-limit.requests-per-minute:60}") int requestsPerMinute,
            @Value("${rate-limit.cleanup-interval-minutes:10}") long cleanupIntervalMinutes) {
        this.maxTokens = requestsPerMinute;
        this.cleanupIntervalMinutes = cleanupIntervalMinutes;

        cleanupExecutor.scheduleAtFixedRate(this::cleanupStaleBuckets,
                cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(httpRequest);
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(maxTokens));

        if (bucket.tryConsume()) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            long waitSeconds = bucket.secondsUntilNextToken();
            httpResponse.setStatus(429);
            httpResponse.setHeader("Retry-After", String.valueOf(waitSeconds));
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Too many requests. Try again in " + waitSeconds + " seconds.\"}");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void cleanupStaleBuckets() {
        long cutoff = System.nanoTime() - TimeUnit.MINUTES.toNanos(cleanupIntervalMinutes);
        buckets.entrySet().removeIf(entry -> entry.getValue().getLastAccessNano() < cutoff);
    }

    @Override
    public void destroy() {
        cleanupExecutor.shutdownNow();
    }

    private static class TokenBucket {
        private final int maxTokens;
        private final double refillRatePerNano;
        private double tokens;
        private long lastRefillNano;
        private long lastAccessNano;

        TokenBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            this.refillRatePerNano = maxTokens / (60.0 * 1_000_000_000L);
            this.tokens = maxTokens;
            this.lastRefillNano = System.nanoTime();
            this.lastAccessNano = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            lastAccessNano = System.nanoTime();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized long secondsUntilNextToken() {
            refill();
            if (tokens >= 1.0) return 0;
            double deficit = 1.0 - tokens;
            return Math.max(1, (long) Math.ceil(deficit / (refillRatePerNano * 1_000_000_000L)));
        }

        synchronized long getLastAccessNano() {
            return lastAccessNano;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNano;
            if (elapsed > 0) {
                tokens = Math.min(maxTokens, tokens + elapsed * refillRatePerNano);
                lastRefillNano = now;
            }
        }
    }
}
