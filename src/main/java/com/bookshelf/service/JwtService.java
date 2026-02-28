package com.bookshelf.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for creating and validating JSON Web Tokens (JWTs).
 *
 * This service handles two types of tokens:
 * - Access tokens: Short-lived (default 15 minutes), sent with every API request
 *   in the Authorization header to prove the user is authenticated.
 * - Refresh tokens: Long-lived (default 7 days), used to obtain new access tokens
 *   without requiring the user to log in again.
 *
 * Tokens are signed using HMAC-SHA256 with a secret key configured in application.yml.
 * The secret key must be Base64-encoded and at least 256 bits long.
 *
 * How JWT works in this app:
 * 1. User logs in with username/password → receives access + refresh tokens
 * 2. Frontend stores tokens and sends access token with every API request
 * 3. When access token expires, frontend uses refresh token to get a new one
 * 4. When refresh token expires, user must log in again
 */
@Service
public class JwtService {

    /** Base64-encoded secret key used to sign and verify all JWTs */
    @Value("${bookshelf.jwt.secret}")
    private String secretKey;

    /** How long access tokens are valid, in milliseconds (default: 900000 = 15 minutes) */
    @Value("${bookshelf.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /** How long refresh tokens are valid, in milliseconds (default: 604800000 = 7 days) */
    @Value("${bookshelf.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Generates a short-lived access token for the given username.
     * This token is sent with every API request in the Authorization header.
     *
     * @param username the username to embed in the token's "subject" claim
     * @return a signed JWT string
     */
    public String generateAccessToken(String username) {
        return buildToken(new HashMap<>(), username, accessTokenExpiration);
    }

    /**
     * Generates a long-lived refresh token for the given username.
     * This token is stored in the database and used to get new access tokens.
     *
     * @param username the username to embed in the token's "subject" claim
     * @return a signed JWT string
     */
    public String generateRefreshToken(String username) {
        return buildToken(new HashMap<>(), username, refreshTokenExpiration);
    }

    /**
     * Extracts the username (subject claim) from a JWT token.
     *
     * @param token the JWT string to parse
     * @return the username stored in the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Checks if a token is valid: not expired and matches the expected username.
     *
     * @param token    the JWT string to validate
     * @param username the expected username to match against the token's subject
     * @return true if the token is valid and belongs to the given user
     */
    public boolean isTokenValid(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return tokenUsername.equals(username) && !isTokenExpired(token);
    }

    /**
     * Checks if a token has expired by comparing its expiration date to now.
     *
     * @param token the JWT string to check
     * @return true if the token's expiration date is in the past
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token the JWT string to parse
     * @return the expiration date embedded in the token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Returns the configured access token expiration time in milliseconds.
     * Used by AuthService to include the expiresIn value in the auth response.
     *
     * @return access token TTL in milliseconds
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Extracts a single claim from the token using the provided resolver function.
     * Claims are the key-value pairs stored inside the JWT payload (e.g., subject, expiration).
     *
     * @param token          the JWT string to parse
     * @param claimsResolver a function that picks out the desired claim from all claims
     * @return the resolved claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the JWT and extracts all claims (payload data).
     * This also verifies the token's signature using our secret key —
     * if the token was tampered with, this will throw an exception.
     *
     * @param token the JWT string to parse and verify
     * @return all claims contained in the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // Verify the token was signed with our secret
                .build()
                .parseSignedClaims(token)       // Parse and validate the token
                .getPayload();                  // Return the claims (payload)
    }

    /**
     * Builds a JWT token with the given extra claims, subject (username), and expiration.
     *
     * The token contains:
     * - "sub" (subject): the username
     * - "iat" (issued at): current timestamp
     * - "exp" (expiration): current time + expiration duration
     * - Any additional claims passed in the extraClaims map
     *
     * @param extraClaims additional key-value pairs to include in the token payload
     * @param username    the username to set as the token's subject
     * @param expiration  how long the token is valid, in milliseconds
     * @return a signed JWT string
     */
    private String buildToken(Map<String, Object> extraClaims, String username, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)                                       // Add any extra claims
                .subject(username)                                         // Set the username as subject
                .issuedAt(new Date(System.currentTimeMillis()))            // Token creation time
                .expiration(new Date(System.currentTimeMillis() + expiration))  // Token expiry time
                .signWith(getSigningKey())                                 // Sign with our secret key
                .compact();                                                // Build the final JWT string
    }

    /**
     * Decodes the Base64-encoded secret key and creates an HMAC-SHA signing key.
     * This key is used both for signing new tokens and verifying existing ones.
     *
     * @return the SecretKey used for HMAC-SHA256 signing
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
