package com.bookshelf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Request DTO for login and registration endpoints.
 *
 * Contains the username and password submitted by the user.
 * Validation ensures both fields are present and the password
 * meets a minimum length requirement.
 *
 * Used by:
 * - POST /api/auth/register — to create the first (and only) user
 * - POST /api/auth/login — to authenticate and receive JWT tokens
 */
public class AuthRequest {

    /** The username to register or log in with — cannot be blank */
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must be at most 50 characters")
    private String username;

    /** The plaintext password — must be at least 8 characters for security */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /** No-arg constructor required for JSON deserialization */
    public AuthRequest() {
    }

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthRequest that = (AuthRequest) o;
        return Objects.equals(username, that.username) &&
                Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public String toString() {
        // Intentionally omit password from toString to avoid logging sensitive data
        return "AuthRequest(username=" + username + ")";
    }
}
