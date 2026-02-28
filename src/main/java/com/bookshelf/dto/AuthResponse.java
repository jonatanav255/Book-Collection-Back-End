package com.bookshelf.dto;

import java.util.Objects;

/**
 * Response DTO returned after successful login or token refresh.
 *
 * Contains the JWT tokens the frontend needs to authenticate API requests:
 * - accessToken: Short-lived token sent in the Authorization header on every request
 * - refreshToken: Long-lived token used to get a new access token when it expires
 * - username: The authenticated user's username (for display purposes)
 * - expiresIn: How many milliseconds until the access token expires
 *   (so the frontend knows when to refresh)
 *
 * Returned by:
 * - POST /api/auth/register
 * - POST /api/auth/login
 * - POST /api/auth/refresh
 */
public class AuthResponse {

    /** Short-lived JWT access token for authenticating API requests */
    private String accessToken;

    /** Long-lived JWT refresh token for obtaining new access tokens */
    private String refreshToken;

    /** The authenticated user's username */
    private String username;

    /** Milliseconds until the access token expires (e.g., 900000 = 15 minutes) */
    private long expiresIn;

    /** No-arg constructor required for JSON serialization */
    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String refreshToken, String username, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthResponse that = (AuthResponse) o;
        return expiresIn == that.expiresIn &&
                Objects.equals(accessToken, that.accessToken) &&
                Objects.equals(refreshToken, that.refreshToken) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, refreshToken, username, expiresIn);
    }

    @Override
    public String toString() {
        // Intentionally omit token values from toString to avoid logging sensitive data
        return "AuthResponse(username=" + username + ", expiresIn=" + expiresIn + ")";
    }
}
