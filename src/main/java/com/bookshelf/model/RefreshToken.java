package com.bookshelf.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a refresh token stored in the database.
 *
 * Refresh tokens are long-lived JWT tokens (default 7 days) that allow users
 * to obtain new access tokens without logging in again. They are stored in the
 * database so they can be revoked (e.g., on logout or token rotation).
 *
 * Token rotation flow:
 * 1. User logs in → receives access token + refresh token (stored in DB)
 * 2. Access token expires → frontend sends refresh token to /api/auth/refresh
 * 3. Server validates refresh token, revokes it (sets revoked = true),
 *    issues a new access + refresh token pair
 * 4. If a revoked token is used again, the request is rejected (possible theft)
 *
 * Each token is linked to a user via userId. When the user is deleted,
 * all their refresh tokens are cascade-deleted by the database FK constraint.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    /** Unique identifier for this refresh token record */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The user this refresh token belongs to (FK to users.id) */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** The actual JWT refresh token string — unique across all tokens */
    @Column(length = 500, nullable = false, unique = true)
    private String token;

    /** When this refresh token expires — requests with expired tokens are rejected */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Whether this token has been revoked.
     * A token is revoked when:
     * - It is used in a refresh request (token rotation — old token revoked, new one issued)
     * - The user logs out
     * Revoked tokens cannot be used again.
     */
    @Column(nullable = false)
    private boolean revoked = false;

    /** When this token was created/issued */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** No-arg constructor required by JPA */
    public RefreshToken() {
    }

    /**
     * Creates a new refresh token record.
     *
     * @param userId    the ID of the user this token belongs to
     * @param token     the JWT refresh token string
     * @param expiresAt when this token expires
     */
    public RefreshToken(UUID userId, String token, LocalDateTime expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
