package com.bookshelf.exception;

/**
 * Thrown when a login attempt fails due to wrong username or password.
 *
 * Maps to a 401 Unauthorized response. The error message intentionally
 * does NOT specify whether the username or password was wrong — this
 * prevents attackers from discovering valid usernames.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
