-- ============================================================================
-- V4: Create refresh_tokens table for JWT token rotation
-- ============================================================================
-- Refresh tokens are long-lived tokens (default 7 days) stored in the database.
-- When a user's short-lived access token expires, they send their refresh token
-- to get a new access token without logging in again.
--
-- Token rotation: Each time a refresh token is used, the old one is revoked
-- (revoked = TRUE) and a new one is issued. This limits the damage if a
-- refresh token is stolen — the attacker can only use it once before it's
-- invalidated.
--
-- Tokens are linked to a user via user_id (FK to users table).
-- When a user is deleted, all their refresh tokens are cascade-deleted.
-- ============================================================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,                              -- Unique identifier for this refresh token record
    user_id UUID NOT NULL,                            -- The user this token belongs to
    token VARCHAR(500) NOT NULL UNIQUE,               -- The actual JWT refresh token string
    expires_at TIMESTAMP NOT NULL,                    -- When this refresh token expires
    revoked BOOLEAN NOT NULL DEFAULT FALSE,           -- Whether this token has been revoked (used or logged out)
    created_at TIMESTAMP NOT NULL,                    -- When this token was issued

    -- Foreign key: delete all tokens when their user is deleted
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for fast token lookups during refresh requests
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Index for finding all tokens belonging to a user (used during logout/cleanup)
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
