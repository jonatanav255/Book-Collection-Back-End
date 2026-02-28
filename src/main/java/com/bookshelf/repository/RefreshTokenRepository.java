package com.bookshelf.repository;

import com.bookshelf.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity database operations.
 *
 * Provides methods to look up refresh tokens by their token string (used during
 * the token refresh flow), and to delete all tokens for a user (used during
 * logout or account cleanup).
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a refresh token record by its token string.
     * Used during the refresh flow to validate and rotate the token.
     *
     * @param token the JWT refresh token string
     * @return an Optional containing the token record if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Delete all refresh tokens belonging to a specific user.
     * Used during logout to invalidate all sessions, or during account cleanup.
     *
     * @param userId the user's UUID
     */
    void deleteByUserId(UUID userId);
}
