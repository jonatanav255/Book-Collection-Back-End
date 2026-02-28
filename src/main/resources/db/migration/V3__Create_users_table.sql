-- ============================================================================
-- V3: Create users table for JWT authentication
-- ============================================================================
-- This is a single-user app, but we store the user in a table so we can
-- validate credentials on login and lock registration after the first user.
-- The password_hash column stores a BCrypt-encoded password (60+ chars).
-- ============================================================================

CREATE TABLE users (
    id UUID PRIMARY KEY,                          -- Unique user identifier
    username VARCHAR(50) NOT NULL UNIQUE,          -- Login username (max 50 chars, must be unique)
    password_hash VARCHAR(255) NOT NULL,           -- BCrypt-hashed password
    created_at TIMESTAMP NOT NULL,                 -- When the user was created
    updated_at TIMESTAMP                           -- Last time the user record was modified
);

-- Index on username for fast lookups during login
CREATE INDEX idx_users_username ON users(username);
