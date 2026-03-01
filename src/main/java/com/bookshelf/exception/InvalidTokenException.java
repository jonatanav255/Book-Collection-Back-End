package com.bookshelf.exception;

/**
 * Thrown when a refresh token is invalid, expired, or has been revoked.
 *
 * This can happen when:
 * - The refresh token string doesn't exist in the database
 * - The refresh token has been revoked (e.g., after logout or token rotation)
 * - The refresh token has expired (past its 7-day TTL)
 *
 * Maps to a 401 Unauthorized response.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
