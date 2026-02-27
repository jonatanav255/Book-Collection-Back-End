package com.bookshelf.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Per-IP rate limiter using the token bucket algorithm. Only applies to /api/ routes.
// Each IP gets a bucket that refills at a fixed rate (default 60 requests/min).
@Component
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // Maps each client IP to its own token bucket
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    // Background thread that removes inactive buckets to prevent memory leaks
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limit-cleanup");
        t.setDaemon(true); // Won't block JVM shutdown
        return t;
    });

    private final int maxTokens;
    private final long cleanupIntervalMinutes;

    // Values come from application.properties (with defaults: 60 req/min, cleanup every 10 min)
    public RateLimitFilter(
            @Value("${rate-limit.requests-per-minute:60}") int requestsPerMinute,
            @Value("${rate-limit.cleanup-interval-minutes:10}") long cleanupIntervalMinutes) {
        this.maxTokens = requestsPerMinute;
        this.cleanupIntervalMinutes = cleanupIntervalMinutes;

        // Schedule periodic removal of buckets from IPs that haven't made requests recently
        cleanupExecutor.scheduleAtFixedRate(this::cleanupStaleBuckets,
                cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);
    }

    // Called by Tomcat for every request (3 params: request, response, chain)
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Skip rate limiting for non-API routes (static files, health checks, etc.)
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response); // FilterChain.doFilter (2 params) — passes to next filter
            return;
        }

        // Skip rate limiting for static asset endpoints (thumbnails, PDFs, audio)
        // These are read-only file serving routes that dominate request count during normal browsing
        if (path.matches(".*/books/[^/]+/(thumbnail|pdf)$") || path.contains("/audio")) {
            chain.doFilter(request, response);
            return;
        }

        // Get or create a token bucket for this IP
        String clientIp = resolveClientIp(httpRequest);
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(maxTokens));

        if (bucket.tryConsume()) {
            // Token available — allow the request through
            chain.doFilter(request, response);
        } else {
            // No tokens left — reject with 429 and tell client when to retry
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            long waitSeconds = bucket.secondsUntilNextToken();
            httpResponse.setStatus(429);
            httpResponse.setHeader("Retry-After", String.valueOf(waitSeconds));
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Too many requests. Try again in " + waitSeconds + " seconds.\"}");
        }
    }

    // Extracts the real client IP, checking X-Forwarded-For for proxied requests
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim(); // First IP is the original client
        }
        return request.getRemoteAddr();
    }

    // Removes buckets from IPs that haven't made requests within the cleanup interval
    private void cleanupStaleBuckets() {
        long cutoff = System.nanoTime() - TimeUnit.MINUTES.toNanos(cleanupIntervalMinutes);
        buckets.entrySet().removeIf(entry -> entry.getValue().getLastAccessNano() < cutoff);
    }

    @Override
    public void destroy() {
        cleanupExecutor.shutdownNow();
    }

    // Token bucket algorithm: starts full, each request consumes 1 token,
    // tokens refill gradually over time (maxTokens per 60 seconds)
    private static class TokenBucket {
        private final int maxTokens;
        private final double refillRatePerNano; // Tokens added per nanosecond
        private double tokens;
        private long lastRefillNano;
        private long lastAccessNano;

        TokenBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            // Convert requests-per-minute to a per-nanosecond refill rate
            this.refillRatePerNano = maxTokens / (60.0 * 1_000_000_000L);
            this.tokens = maxTokens; // Start with a full bucket
            this.lastRefillNano = System.nanoTime();
            this.lastAccessNano = System.nanoTime();
        }

        // Try to use 1 token. Returns true if allowed, false if rate limited.
        synchronized boolean tryConsume() {
            refill();
            lastAccessNano = System.nanoTime();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        // Calculates how many seconds until at least 1 token is available
        synchronized long secondsUntilNextToken() {
            refill();
            if (tokens >= 1.0) return 0;
            double deficit = 1.0 - tokens;
            return Math.max(1, (long) Math.ceil(deficit / (refillRatePerNano * 1_000_000_000L)));
        }

        synchronized long getLastAccessNano() {
            return lastAccessNano;
        }

        // Adds tokens based on elapsed time since last refill, capped at maxTokens
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
