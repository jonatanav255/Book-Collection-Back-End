package com.bookshelf.controller;

import com.bookshelf.dto.AuthRequest;
import com.bookshelf.dto.AuthResponse;
import com.bookshelf.dto.RefreshTokenRequest;
import com.bookshelf.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 *
 * All endpoints under /api/auth/ are PUBLIC — they do not require
 * an existing JWT token to access (configured in SecurityConfig in Phase 4).
 *
 * Provides 4 endpoints:
 * - POST /api/auth/register — create the first (and only) user account
 * - POST /api/auth/login    — authenticate and receive JWT tokens
 * - POST /api/auth/refresh  — exchange a refresh token for new tokens
 * - POST /api/auth/logout   — revoke a refresh token to end the session
 *
 * This controller is a thin layer — all business logic lives in AuthService.
 * The controller's job is to:
 * 1. Accept HTTP requests and validate the input (@Valid)
 * 2. Delegate to AuthService for the actual work
 * 3. Return the appropriate HTTP status code and response body
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    /**
     * Constructor injection — Spring provides the AuthService instance automatically.
     *
     * @param authService the service that handles all auth business logic
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user account.
     *
     * This endpoint is only usable ONCE — after the first user is created,
     * all subsequent calls return 403 Forbidden (single-user app).
     *
     * Request body: { "username": "admin", "password": "mypassword123" }
     * Success response (201): { "accessToken": "...", "refreshToken": "...", "username": "admin", "expiresIn": 900000 }
     * Locked response (403): { "error": "Forbidden", "message": "Registration is locked..." }
     *
     * @param request the username and password for the new account
     * @return 201 Created with tokens, or 403 if registration is locked
     */
    @Operation(summary = "Register a new user (only works once — single-user app)")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        // Delegate to AuthService — it handles the single-user lock check,
        // password hashing, user creation, and token generation
        AuthResponse response = authService.register(request);

        // Return 201 Created (not 200 OK) because a new resource (user) was created
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Log in with username and password.
     *
     * Validates credentials against the stored user and returns JWT tokens.
     *
     * Request body: { "username": "admin", "password": "mypassword123" }
     * Success response (200): { "accessToken": "...", "refreshToken": "...", "username": "admin", "expiresIn": 900000 }
     * Failed response (401): { "error": "Unauthorized", "message": "Invalid username or password" }
     *
     * @param request the username and password to authenticate
     * @return 200 OK with tokens, or 401 if credentials are wrong
     */
    @Operation(summary = "Log in with username and password to receive JWT tokens")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        // Delegate to AuthService — it looks up the user, verifies the password,
        // and generates tokens if credentials are valid
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get new tokens using a valid refresh token.
     *
     * Called by the frontend when the access token has expired (after 15 min).
     * The old refresh token is revoked (token rotation) and a new pair is issued.
     *
     * Request body: { "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
     * Success response (200): { "accessToken": "...", "refreshToken": "...", "username": "admin", "expiresIn": 900000 }
     * Failed response (401): { "error": "Unauthorized", "message": "Refresh token has been revoked" }
     *
     * @param request the refresh token to exchange for new tokens
     * @return 200 OK with new tokens, or 401 if refresh token is invalid/revoked/expired
     */
    @Operation(summary = "Exchange a refresh token for new access and refresh tokens")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        // Delegate to AuthService — it validates the refresh token,
        // revokes the old one, and generates a new token pair
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Log out by revoking the refresh token.
     *
     * After this call, the refresh token can no longer be used to get new access tokens.
     * Note: The current access token will still work until it naturally expires (max 15 min).
     * This is a standard tradeoff with stateless JWT auth.
     *
     * This endpoint is idempotent — calling it multiple times with the same token
     * or a non-existent token will not cause errors (always returns 204).
     *
     * Request body: { "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
     * Response: 204 No Content (always, regardless of whether the token was found)
     *
     * @param request the refresh token to revoke
     * @return 204 No Content
     */
    @Operation(summary = "Log out by revoking the refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        // Delegate to AuthService — it finds and revokes the token if it exists
        authService.logout(request);

        // Return 204 No Content — logout was successful, nothing to return in the body
        return ResponseEntity.noContent().build();
    }
}
