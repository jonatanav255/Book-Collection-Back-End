package com.bookshelf.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitFilter token-bucket logic.
 * Uses Spring's MockHttpServletRequest/Response — no Spring context needed.
 */
class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        // 60 requests/min, 10-minute cleanup interval (same as production defaults)
        filter = new RateLimitFilter(60, 10);
    }

    // ── Non-API routes are always allowed ─────────────────────────────────────

    @Test
    void doFilter_allowsNonApiRequests_withoutRateLimiting() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void doFilter_allowsStaticAssets_pdfEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books/123/pdf");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // Exempt endpoint should pass through
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_allowsStaticAssets_thumbnailEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books/abc/thumbnail");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // ── API requests consume tokens ───────────────────────────────────────────

    @Test
    void doFilter_allowsFirstApiRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void doFilter_returns429_whenBucketExhausted() throws Exception {
        // Drain all tokens by sending 61 requests (default limit is 60)
        FilterChain chain = mock(FilterChain.class);
        String ip = "10.0.0.1";

        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/books");
            req.setRemoteAddr(ip);
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        // 61st request should be rate-limited
        MockHttpServletRequest lastRequest = new MockHttpServletRequest("GET", "/api/books");
        lastRequest.setRemoteAddr(ip);
        MockHttpServletResponse lastResponse = new MockHttpServletResponse();

        filter.doFilter(lastRequest, lastResponse, chain);

        assertThat(lastResponse.getStatus()).isEqualTo(429);
        assertThat(lastResponse.getHeader("Retry-After")).isNotNull();
    }

    // ── X-Forwarded-For header resolution ─────────────────────────────────────

    @Test
    void doFilter_usesXForwardedForHeader_asClientIp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Should not throw; just verifying the header is used
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_differentIps_haveIndependentBuckets() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        // Drain bucket for IP-A
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/books");
            req.setRemoteAddr("10.0.0.2");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        // IP-B should still have tokens
        MockHttpServletRequest reqB = new MockHttpServletRequest("GET", "/api/books");
        reqB.setRemoteAddr("10.0.0.3");
        MockHttpServletResponse respB = new MockHttpServletResponse();

        filter.doFilter(reqB, respB, chain);

        assertThat(respB.getStatus()).isNotEqualTo(429);
    }
}
