package com.bookshelf.service;

import com.bookshelf.dto.AuthRequest;
import com.bookshelf.dto.AuthResponse;
import com.bookshelf.dto.RefreshTokenRequest;
import com.bookshelf.exception.InvalidCredentialsException;
import com.bookshelf.exception.InvalidTokenException;
import com.bookshelf.exception.RegistrationLockedException;
import com.bookshelf.model.RefreshToken;
import com.bookshelf.model.User;
import com.bookshelf.repository.RefreshTokenRepository;
import com.bookshelf.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service that handles all authentication business logic:
 * - Registration (with single-user lock)
 * - Login (credential validation)
 * - Token refresh (with token rotation)
 * - Logout (token revocation)
 *
 * This is the orchestrator — it coordinates between:
 * - UserRepository: to store/look up user accounts
 * - RefreshTokenRepository: to store/revoke refresh tokens
 * - JwtService: to generate/validate JWT token strings
 * - PasswordEncoder: to hash passwords on registration and verify on login
 *
 * SINGLE-USER LOCK: This app only allows one user. After the first user registers,
 * all subsequent registration attempts are rejected with a 403 Forbidden.
 *
 * TOKEN ROTATION: Every time a refresh token is used, the old one is revoked and
 * a new one is issued. This means if an attacker steals a refresh token,
 * they can only use it once — after that, the legitimate user's next refresh
 * will fail (because the token was already used), alerting them to the theft.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor injection — Spring automatically provides these dependencies.
     * We use constructor injection (not @Autowired on fields) because:
     * 1. It makes dependencies explicit and visible
     * 2. It makes the class easier to test (just pass mocks to the constructor)
     * 3. Fields can be final, preventing accidental reassignment
     */
    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user account.
     *
     * This is only allowed when NO users exist yet (single-user app).
     * Once the first user registers, this endpoint is permanently locked.
     *
     * Steps:
     * 1. Check if any user already exists → if yes, throw RegistrationLockedException (403)
     * 2. Hash the plaintext password using BCrypt (never store plaintext passwords!)
     * 3. Save the new user to the database
     * 4. Generate access + refresh tokens for immediate login
     * 5. Store the refresh token in the database
     * 6. Return the tokens to the client
     *
     * @param request contains the username and plaintext password
     * @return AuthResponse with access token, refresh token, username, and expiry info
     * @throws RegistrationLockedException if a user already exists
     */
    @Transactional
    public AuthResponse register(AuthRequest request) {
        // Single-user lock: only allow registration if no users exist
        if (userRepository.count() > 0) {
            throw new RegistrationLockedException("Registration is locked. A user already exists.");
        }

        // Hash the password with BCrypt before storing — BCrypt automatically generates
        // a random salt and includes it in the hash, so identical passwords produce
        // different hashes. The result looks like: $2a$10$N9qo8uLOickgx2ZMRZoMye...
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Create and save the user entity
        User user = new User(request.getUsername(), hashedPassword);
        userRepository.save(user);

        // Generate tokens so the user is immediately logged in after registering
        return generateTokensAndBuildResponse(user);
    }

    /**
     * Authenticates a user with their username and password.
     *
     * Steps:
     * 1. Look up the user by username → if not found, throw InvalidCredentialsException
     * 2. Verify the password matches the stored hash → if not, throw InvalidCredentialsException
     * 3. Generate access + refresh tokens
     * 4. Store the refresh token in the database
     * 5. Return the tokens to the client
     *
     * Note: We use the same error message for "user not found" and "wrong password"
     * so attackers can't determine whether a username exists.
     *
     * @param request contains the username and plaintext password
     * @return AuthResponse with tokens
     * @throws InvalidCredentialsException if username doesn't exist or password is wrong
     */
    @Transactional
    public AuthResponse login(AuthRequest request) {
        // Find the user by username — if not found, throw generic "invalid credentials"
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        // Verify the plaintext password matches the stored BCrypt hash
        // passwordEncoder.matches() handles the salt extraction and comparison internally
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Credentials are valid — generate tokens
        return generateTokensAndBuildResponse(user);
    }

    /**
     * Issues new tokens using a valid refresh token (token rotation).
     *
     * This is called when the frontend's access token has expired and it
     * needs a new one without making the user log in again.
     *
     * TOKEN ROTATION STEPS:
     * 1. Find the refresh token in the database → if not found, reject
     * 2. Check it hasn't been revoked → if revoked, reject (possible token theft)
     * 3. Check it hasn't expired → if expired, reject (user must log in again)
     * 4. Revoke the old refresh token (mark revoked = true in the database)
     * 5. Look up the user to generate new tokens
     * 6. Generate new access + refresh tokens
     * 7. Store the new refresh token in the database
     * 8. Return the new tokens
     *
     * WHY ROTATE? If an attacker steals a refresh token, they race with the
     * legitimate user to use it first. Whoever uses it second gets rejected,
     * which signals that something is wrong.
     *
     * @param request contains the refresh token string
     * @return AuthResponse with new tokens
     * @throws InvalidTokenException if the refresh token is invalid, revoked, or expired
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Look up the refresh token in the database
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Check if the token has been revoked (already used or logged out)
        if (storedToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        // Check if the token has expired (past its 7-day TTL)
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Revoke the old token — it can never be used again (token rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Look up the user to generate new tokens
        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found for refresh token"));

        // Generate new token pair
        return generateTokensAndBuildResponse(user);
    }

    /**
     * Logs out the user by revoking their refresh token.
     *
     * After logout, the refresh token can no longer be used to get new access tokens.
     * The access token will still work until it naturally expires (max 15 minutes),
     * but the user can't renew it. This is a tradeoff of stateless JWT auth —
     * true instant invalidation of access tokens would require a token blacklist
     * (checked on every request), which adds complexity and database load.
     *
     * @param request contains the refresh token to revoke
     */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        // Find and revoke the refresh token if it exists
        // We don't throw an error if the token is not found — logout should be idempotent
        // (calling logout multiple times should not cause errors)
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Generates access + refresh tokens for a user and saves the refresh token to the DB.
     *
     * This is the common logic shared by register, login, and refreshToken —
     * all three end with "generate tokens and return them."
     *
     * @param user the authenticated user to generate tokens for
     * @return AuthResponse containing both tokens, username, and expiry info
     */
    private AuthResponse generateTokensAndBuildResponse(User user) {
        // Generate the short-lived access token (15 min) — not stored in DB
        String accessToken = jwtService.generateAccessToken(user.getUsername());

        // Generate the long-lived refresh token (7 days) — will be stored in DB
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        // Calculate when the refresh token expires so we can store it in the DB
        // We add the refresh token expiration (in ms, converted to seconds) to now
        LocalDateTime refreshExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtService.getAccessTokenExpiration() / 1000 * 7 * 24);

        // Persist the refresh token in the database so we can validate/revoke it later
        RefreshToken refreshTokenEntity = new RefreshToken(
                user.getId(),       // Which user this token belongs to
                refreshToken,        // The actual JWT string
                refreshExpiresAt     // When it expires
        );
        refreshTokenRepository.save(refreshTokenEntity);

        // Build and return the response with both tokens
        return new AuthResponse(
                accessToken,                            // For authenticating API requests
                refreshToken,                           // For getting new access tokens
                user.getUsername(),                      // So the frontend knows who's logged in
                jwtService.getAccessTokenExpiration()    // So the frontend knows when to refresh
        );
    }
}
