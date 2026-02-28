package com.bookshelf.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity for authentication.
 *
 * Represents a single application user stored in the "users" table.
 * This is a single-user app — only one user can register. After the first
 * user is created, registration is locked (enforced by AuthService in Phase 3).
 *
 * The passwordHash field stores a BCrypt-encoded password, never plaintext.
 */
@Entity
@Table(name = "users")
public class User {

    /** Unique identifier for the user, auto-generated as UUID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Login username — must be unique, max 50 characters */
    @Column(length = 50, nullable = false, unique = true)
    private String username;

    /** BCrypt-hashed password — never store or return plaintext passwords */
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    /** Timestamp when this user was created, set automatically by Hibernate */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when this user was last updated, set automatically by Hibernate */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** No-arg constructor required by JPA */
    public User() {
    }

    /**
     * Creates a new User with the given username and hashed password.
     *
     * @param username     the login username
     * @param passwordHash the BCrypt-encoded password
     */
    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
