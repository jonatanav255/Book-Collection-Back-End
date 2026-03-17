package com.bookshelf.controller;

import com.bookshelf.config.JwtAuthenticationFilter;
import com.bookshelf.config.SecurityConfig;
import com.bookshelf.dto.AuthRequest;
import com.bookshelf.dto.AuthResponse;
import com.bookshelf.dto.RefreshTokenRequest;
import com.bookshelf.exception.GlobalExceptionHandler;
import com.bookshelf.exception.InvalidCredentialsException;
import com.bookshelf.exception.InvalidTokenException;
import com.bookshelf.exception.RegistrationLockedException;
import com.bookshelf.service.AuthService;
import com.bookshelf.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the AuthController + Spring Security filter chain.
 *
 * These tests verify the FULL request lifecycle through:
 * MockMvc → Security Filter Chain → JwtAuthenticationFilter → Controller → GlobalExceptionHandler
 *
 * Unlike unit tests (which test classes in isolation with mocks), these tests
 * load the actual Spring Security configuration and verify that:
 * - Auth endpoints (/api/auth/**) are accessible WITHOUT a token
 * - Protected endpoints return 401 WITHOUT a token
 * - Protected endpoints return 200 WITH a valid token
 * - Error responses are proper JSON with correct status codes
 *
 * @WebMvcTest: Loads only the web layer (controller + security config), NOT the full app.
 *   This means no database, no repositories, no services — just the HTTP handling.
 *   All services are mocked with @MockBean.
 *
 * @Import: Explicitly imports SecurityConfig and JwtAuthenticationFilter because
 *   @WebMvcTest only auto-detects @Controller classes, not @Configuration or @Component.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AuthControllerIntegrationTest {

    /** MockMvc simulates HTTP requests without starting a real server */
    @Autowired
    private MockMvc mockMvc;

    /** Jackson ObjectMapper for converting Java objects to JSON strings */
    @Autowired
    private ObjectMapper objectMapper;

    /** Mocked AuthService — we control what it returns/throws in each test */
    @MockBean
    private AuthService authService;

    /**
     * Mocked JwtService — needed because JwtAuthenticationFilter depends on it.
     * In tests where we send a Bearer token, we configure this mock to validate it.
     */
    @MockBean
    private JwtService jwtService;

    // ── Register ────────────────────────────────────────────────────────────

    @Test
    void register_returns201_whenSuccessful() throws Exception {
        // Arrange: AuthService returns tokens for a new user
        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", "admin", 900000);
        when(authService.register(any(AuthRequest.class))).thenReturn(authResponse);

        // Act & Assert: POST /api/auth/register without any token → 201 Created
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("admin", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.expiresIn").value(900000));
    }

    @Test
    void register_returns403_whenRegistrationIsLocked() throws Exception {
        // Arrange: a user already exists, so registration is locked
        when(authService.register(any(AuthRequest.class)))
                .thenThrow(new RegistrationLockedException("Registration is locked. A user already exists."));

        // Act & Assert: should return 403 Forbidden with JSON error
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("newuser", "password123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Registration is locked. A user already exists."));
    }

    @Test
    void register_returns400_whenValidationFails() throws Exception {
        // Act & Assert: send request with blank username and short password → 400
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    // ── Login ───────────────────────────────────────────────────────────────

    @Test
    void login_returns200_withValidCredentials() throws Exception {
        // Arrange
        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", "admin", 900000);
        when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("admin", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void login_returns401_withInvalidCredentials() throws Exception {
        // Arrange
        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid username or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("admin", "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    // ── Refresh ─────────────────────────────────────────────────────────────

    @Test
    void refresh_returns200_withValidRefreshToken() throws Exception {
        // Arrange
        AuthResponse authResponse = new AuthResponse("new-access", "new-refresh", "admin", 900000);
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("old-refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_returns401_withRevokedToken() throws Exception {
        // Arrange
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new InvalidTokenException("Refresh token has been revoked"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("revoked-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    // ── Logout ──────────────────────────────────────────────────────────────

    @Test
    void logout_returns204() throws Exception {
        // Arrange: logout always succeeds (idempotent)
        doNothing().when(authService).logout(any(RefreshTokenRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("some-token"))))
                .andExpect(status().isNoContent());
    }

    // ── Protected endpoint access ───────────────────────────────────────────

    @Test
    void protectedEndpoint_returns401_withoutToken() throws Exception {
        // Act & Assert: accessing /api/books without a Bearer token → 401
        // This proves the security filter chain is working — unauthenticated
        // requests to non-auth endpoints are rejected.
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void protectedEndpoint_passesSecurityWithValidToken() throws Exception {
        // Arrange: configure JwtService mock to accept our test token
        when(jwtService.extractUsername("valid-access-token")).thenReturn("admin");
        when(jwtService.isTokenValid("valid-access-token", "admin")).thenReturn(true);

        // Act & Assert: accessing /api/books with a valid Bearer token.
        // The key assertion: the response is NOT 401 Unauthorized.
        // That proves the request got PAST the security filter chain.
        // It won't be 200 because BookController isn't loaded in this test slice
        // (@WebMvcTest only loads AuthController). But any non-401 status means
        // the JWT was accepted and security let the request through.
        mockMvc.perform(get("/api/books")
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // The request must NOT be rejected by security (401).
                    // Any other status (404, 500, etc.) means it passed auth.
                    assert status != 401 : "Expected to pass security, but got 401 Unauthorized";
                });
    }

    @Test
    void protectedEndpoint_returns401_withInvalidToken() throws Exception {
        // Arrange: JwtService says this token is invalid
        when(jwtService.extractUsername("bad-token")).thenReturn("admin");
        when(jwtService.isTokenValid("bad-token", "admin")).thenReturn(false);

        // Act & Assert: invalid token → treated as unauthenticated → 401
        mockMvc.perform(get("/api/books")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_returns401_withExpiredToken() throws Exception {
        // Arrange: extractUsername throws (simulating expired/malformed token)
        when(jwtService.extractUsername("expired-token")).thenThrow(new RuntimeException("Token expired"));

        // Act & Assert: exception caught by filter, request proceeds as unauthenticated → 401
        mockMvc.perform(get("/api/books")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }

    // ── Status ─────────────────────────────────────────────────────────────

    @Test
    void status_returnsRegistrationOpenTrue_whenNoUserExists() throws Exception {
        // Arrange: no users registered yet
        when(authService.isUserRegistered()).thenReturn(false);

        // Act & Assert: GET /api/auth/status → 200 with registrationOpen = true
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationOpen").value(true));
    }

    @Test
    void status_returnsRegistrationOpenFalse_whenUserExists() throws Exception {
        // Arrange: a user is already registered
        when(authService.isUserRegistered()).thenReturn(true);

        // Act & Assert: GET /api/auth/status → 200 with registrationOpen = false
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationOpen").value(false));
    }

    @Test
    void status_isPublicEndpoint() throws Exception {
        // Arrange: no users registered (arbitrary — the point is no auth token is sent)
        when(authService.isUserRegistered()).thenReturn(false);

        // Act & Assert: GET /api/auth/status without any Authorization header → 200
        // This proves the endpoint is accessible without authentication,
        // just like the other /api/auth/** endpoints.
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk());
    }
}
