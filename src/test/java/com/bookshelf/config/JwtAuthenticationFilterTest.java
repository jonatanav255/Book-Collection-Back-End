package com.bookshelf.config;

import com.bookshelf.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 *
 * Tests that the filter correctly:
 * - Sets authentication when a valid Bearer token is present
 * - Passes through without setting auth when no token is present
 * - Passes through without setting auth when the token is invalid/expired
 * - Always calls filterChain.doFilter() (never blocks the request)
 *
 * Uses MockHttpServletRequest/Response to simulate HTTP requests
 * without starting a web server.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        // Create fresh mock request/response for each test
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // Clear any leftover authentication from previous tests
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Always clean up SecurityContext after each test to avoid test pollution
        SecurityContextHolder.clearContext();
    }

    // ── Valid token ─────────────────────────────────────────────────────────

    @Test
    void setsAuthentication_whenValidBearerTokenPresent() throws ServletException, IOException {
        // Arrange: request has a valid Bearer token
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("admin");
        when(jwtService.isTokenValid("valid-token", "admin")).thenReturn(true);

        // Act: run the filter
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert: SecurityContext should now have authentication set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("admin");

        // The filter chain must always be called — the request must continue to the next filter
        verify(filterChain).doFilter(request, response);
    }

    // ── No token ────────────────────────────────────────────────────────────

    @Test
    void doesNotSetAuthentication_whenNoAuthorizationHeader() throws ServletException, IOException {
        // Arrange: request has no Authorization header at all

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert: no authentication should be set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // Filter chain must still be called — unauthenticated requests should pass through
        // (Spring Security will handle the 401 later in the authorization check)
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotSetAuthentication_whenAuthorizationHeaderIsNotBearer() throws ServletException, IOException {
        // Arrange: header exists but uses Basic auth instead of Bearer
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert: no authentication (we only handle Bearer tokens)
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // ── Invalid/expired token ───────────────────────────────────────────────

    @Test
    void doesNotSetAuthentication_whenTokenIsInvalid() throws ServletException, IOException {
        // Arrange: token is present but JwtService says it's invalid
        request.addHeader("Authorization", "Bearer invalid-token");
        when(jwtService.extractUsername("invalid-token")).thenReturn("admin");
        when(jwtService.isTokenValid("invalid-token", "admin")).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert: no authentication should be set for invalid tokens
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotSetAuthentication_whenTokenParsingThrowsException() throws ServletException, IOException {
        // Arrange: extractUsername throws an exception (e.g., malformed or expired token)
        request.addHeader("Authorization", "Bearer malformed-token");
        when(jwtService.extractUsername("malformed-token")).thenThrow(new RuntimeException("Bad token"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert: exception is caught silently, no authentication set
        // The filter must NOT propagate exceptions — it should let the request continue
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
