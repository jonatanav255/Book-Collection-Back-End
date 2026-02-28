package com.bookshelf.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

/**
 * Request DTO for the token refresh endpoint.
 *
 * When the frontend's access token expires, it sends this request with
 * the refresh token to get a new access token without requiring the user
 * to log in again.
 *
 * Used by: POST /api/auth/refresh
 */
public class RefreshTokenRequest {

    /** The refresh token string received from a previous login or refresh response */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    /** No-arg constructor required for JSON deserialization */
    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshTokenRequest that = (RefreshTokenRequest) o;
        return Objects.equals(refreshToken, that.refreshToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refreshToken);
    }

    @Override
    public String toString() {
        // Intentionally omit token value from toString to avoid logging sensitive data
        return "RefreshTokenRequest(refreshToken=***)";
    }
}
