package com.bookshelf.repository;

import com.bookshelf.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity database operations.
 *
 * Provides methods to look up users by username for login validation,
 * check if a username already exists, and the inherited count() method
 * which is used to enforce the single-user registration lock.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their username (used during login).
     *
     * @param username the username to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if a user with the given username already exists.
     *
     * @param username the username to check
     * @return true if a user with that username exists
     */
    boolean existsByUsername(String username);
}
