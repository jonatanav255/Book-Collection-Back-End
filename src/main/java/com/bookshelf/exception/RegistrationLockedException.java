package com.bookshelf.exception;

/**
 * Thrown when someone tries to register a new user but a user already exists.
 *
 * This is a single-user app — only one user account is allowed.
 * After the first user registers, all subsequent registration attempts
 * are rejected with this exception, which maps to a 403 Forbidden response.
 */
public class RegistrationLockedException extends RuntimeException {
    public RegistrationLockedException(String message) {
        super(message);
    }
}
