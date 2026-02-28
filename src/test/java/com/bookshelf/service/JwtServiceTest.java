package com.bookshelf.service;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtService — token generation and validation.
 *
 * These tests verify that:
 * - Access and refresh tokens can be generated and parsed back to extract the username
 * - Token validation correctly accepts valid tokens and rejects wrong usernames
 * - Expired tokens are detected and rejected
 *
 * No Spring context is needed — we set the private fields via reflection
 * to keep tests fast and isolated.
 */
class JwtServiceTest {

    private JwtService jwtService;

    /**
     * Base64-encoded 256-bit test secret key.
     * This is only used in tests — the real key comes from application.yml / env vars.
     */
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhMjU2LXNpZ25pbmc=";

    /** 15 minutes in milliseconds — matches the default access token TTL */
    private static final long ACCESS_EXPIRATION = 900000;

    /** 7 days in milliseconds — matches the default refresh token TTL */
    private static final long REFRESH_EXPIRATION = 604800000;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        // Set private fields via reflection since we're not using Spring context
        setField(jwtService, "secretKey", TEST_SECRET);
        setField(jwtService, "accessTokenExpiration", ACCESS_EXPIRATION);
        setField(jwtService, "refreshTokenExpiration", REFRESH_EXPIRATION);
    }

    // ── generateAccessToken ─────────────────────────────────────────────────

    @Test
    void generateAccessToken_returnsTokenContainingUsername() {
        // Generate an access token and verify the username can be extracted from it
        String token = jwtService.generateAccessToken("admin");
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("admin");
    }

    @Test
    void generateAccessToken_returnsNonExpiredToken() {
        // A freshly generated token should not be expired
        String token = jwtService.generateAccessToken("admin");

        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    // ── generateRefreshToken ────────────────────────────────────────────────

    @Test
    void generateRefreshToken_returnsTokenContainingUsername() {
        // Refresh tokens should also contain the username
        String token = jwtService.generateRefreshToken("admin");
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("admin");
    }

    @Test
    void generateRefreshToken_isDifferentFromAccessToken() {
        // Access and refresh tokens for the same user should be different strings
        // (different expiration times produce different signatures)
        String accessToken = jwtService.generateAccessToken("admin");
        String refreshToken = jwtService.generateRefreshToken("admin");

        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    // ── isTokenValid ────────────────────────────────────────────────────────

    @Test
    void isTokenValid_returnsTrue_forCorrectUsername() {
        // A token should be valid when checked against the same username it was created for
        String token = jwtService.generateAccessToken("admin");

        assertThat(jwtService.isTokenValid(token, "admin")).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_forWrongUsername() {
        // A token should be invalid when checked against a different username
        String token = jwtService.generateAccessToken("admin");

        assertThat(jwtService.isTokenValid(token, "wronguser")).isFalse();
    }

    // ── expired token ───────────────────────────────────────────────────────

    @Test
    void isTokenExpired_returnsTrue_forExpiredToken() throws Exception {
        // Create a JwtService with a very short expiration (1ms) to test expiry
        JwtService shortLivedService = new JwtService();
        setField(shortLivedService, "secretKey", TEST_SECRET);
        setField(shortLivedService, "accessTokenExpiration", 1L);  // 1 millisecond
        setField(shortLivedService, "refreshTokenExpiration", 1L);

        String token = shortLivedService.generateAccessToken("admin");

        // Wait for the token to expire
        Thread.sleep(50);

        // Parsing an expired token should throw ExpiredJwtException
        assertThatThrownBy(() -> shortLivedService.isTokenExpired(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // ── extractExpiration ───────────────────────────────────────────────────

    @Test
    void extractExpiration_returnsDateInTheFuture() {
        // The expiration date of a freshly generated token should be in the future
        String token = jwtService.generateAccessToken("admin");

        assertThat(jwtService.extractExpiration(token)).isAfter(new java.util.Date());
    }

    // ── getAccessTokenExpiration ────────────────────────────────────────────

    @Test
    void getAccessTokenExpiration_returnsConfiguredValue() {
        // Verify the getter returns the configured expiration value
        assertThat(jwtService.getAccessTokenExpiration()).isEqualTo(ACCESS_EXPIRATION);
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    /**
     * Sets a private field on an object via reflection.
     * Used to inject test values without needing a Spring context.
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
