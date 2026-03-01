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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService — registration, login, token refresh, and logout.
 *
 * Uses Mockito to mock all dependencies (repositories, JwtService, PasswordEncoder)
 * so we can test the business logic in isolation without a database or Spring context.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // ── register ────────────────────────────────────────────────────────────

    @Test
    void register_succeeds_whenNoUsersExist() {
        // Arrange: no users exist yet, so registration should be allowed
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateAccessToken("admin")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("admin")).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        AuthResponse response = authService.register(new AuthRequest("admin", "password123"));

        // Assert: should return tokens and username
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getExpiresIn()).isEqualTo(900000L);

        // Verify the user and refresh token were saved
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_throwsRegistrationLocked_whenUserAlreadyExists() {
        // Arrange: a user already exists
        when(userRepository.count()).thenReturn(1L);

        // Act & Assert: should throw RegistrationLockedException (403)
        assertThatThrownBy(() -> authService.register(new AuthRequest("newuser", "password123")))
                .isInstanceOf(RegistrationLockedException.class)
                .hasMessageContaining("Registration is locked");

        // Verify no user was saved
        verify(userRepository, never()).save(any());
    }

    // ── login ───────────────────────────────────────────────────────────────

    @Test
    void login_succeeds_withCorrectCredentials() {
        // Arrange: user exists and password matches
        User user = new User("admin", "$2a$10$hashedpassword");
        user.setId(UUID.randomUUID());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);
        when(jwtService.generateAccessToken("admin")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("admin")).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        AuthResponse response = authService.login(new AuthRequest("admin", "password123"));

        // Assert
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUsername()).isEqualTo("admin");
    }

    @Test
    void login_throwsInvalidCredentials_whenUsernameNotFound() {
        // Arrange: no user with this username
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert: should throw InvalidCredentialsException (401)
        assertThatThrownBy(() -> authService.login(new AuthRequest("unknown", "password123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordIsWrong() {
        // Arrange: user exists but password doesn't match
        User user = new User("admin", "$2a$10$hashedpassword");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(new AuthRequest("admin", "wrongpassword")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    // ── refreshToken ────────────────────────────────────────────────────────

    @Test
    void refreshToken_succeeds_withValidToken() {
        // Arrange: valid, non-revoked, non-expired refresh token
        UUID userId = UUID.randomUUID();
        RefreshToken storedToken = new RefreshToken(userId, "old-refresh-token",
                LocalDateTime.now().plusDays(7));
        User user = new User("admin", "$2a$10$hashedpassword");
        user.setId(userId);

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("admin")).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken("admin")).thenReturn("new-refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);

        // Act
        AuthResponse response = authService.refreshToken(new RefreshTokenRequest("old-refresh-token"));

        // Assert: should return new tokens
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");

        // Verify the old token was revoked
        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    void refreshToken_throwsInvalidToken_whenTokenNotFound() {
        // Arrange: token doesn't exist in the database
        when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("nonexistent")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refreshToken_throwsInvalidToken_whenTokenIsRevoked() {
        // Arrange: token exists but has been revoked (already used or logged out)
        RefreshToken revokedToken = new RefreshToken(UUID.randomUUID(), "revoked-token",
                LocalDateTime.now().plusDays(7));
        revokedToken.setRevoked(true);

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revokedToken));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("revoked-token")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void refreshToken_throwsInvalidToken_whenTokenIsExpired() {
        // Arrange: token exists but has expired (past its TTL)
        RefreshToken expiredToken = new RefreshToken(UUID.randomUUID(), "expired-token",
                LocalDateTime.now().minusDays(1));  // expired yesterday

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("expired-token")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    // ── logout ──────────────────────────────────────────────────────────────

    @Test
    void logout_revokesToken_whenTokenExists() {
        // Arrange: token exists in the database
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "refresh-token",
                LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        authService.logout(new RefreshTokenRequest("refresh-token"));

        // Assert: token should be marked as revoked
        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void logout_doesNotThrow_whenTokenDoesNotExist() {
        // Arrange: token doesn't exist — logout should still succeed (idempotent)
        when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert: should not throw any exception
        authService.logout(new RefreshTokenRequest("nonexistent"));

        // Verify nothing was saved
        verify(refreshTokenRepository, never()).save(any());
    }
}
