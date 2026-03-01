package com.bookshelf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * PHASES 1-3 (current): Temporary "permit all" configuration with PasswordEncoder.
 * The security filter chain still allows all requests through — no authentication
 * is enforced yet. The PasswordEncoder bean was added in Phase 3 so AuthService
 * can hash passwords during registration and verify them during login.
 *
 * Phase 4 will rewrite the filter chain to:
 * - Require authentication on all non-auth endpoints
 * - Add the JWT authentication filter
 * - Configure stateless sessions
 * - Integrate CORS with Spring Security
 * - Add a custom JSON 401 error response
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain.
     * Currently permits all requests — no authentication enforced yet.
     * CSRF is disabled because this is a stateless REST API using JWT tokens.
     *
     * @param http the HttpSecurity builder
     * @return the configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — not needed for stateless JWT-based REST APIs
                .csrf(csrf -> csrf.disable())
                // Temporarily allow all requests (will be locked down in Phase 4)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * Provides a BCrypt password encoder for hashing and verifying passwords.
     *
     * BCrypt is the industry standard for password hashing because:
     * - It automatically generates a random salt (so identical passwords produce different hashes)
     * - It's intentionally slow (making brute-force attacks impractical)
     * - The work factor can be increased over time as hardware gets faster
     *
     * Used by AuthService:
     * - Registration: passwordEncoder.encode("plaintext") → "$2a$10$N9qo8uLO..."
     * - Login: passwordEncoder.matches("plaintext", "$2a$10$N9qo8uLO...") → true/false
     *
     * @return a BCryptPasswordEncoder instance managed by Spring
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
