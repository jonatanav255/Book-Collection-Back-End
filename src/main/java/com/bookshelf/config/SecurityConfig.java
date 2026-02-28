package com.bookshelf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * PHASE 1 (current): Temporary "permit all" configuration.
 * Adding spring-boot-starter-security without any SecurityFilterChain bean
 * would auto-secure every endpoint with HTTP Basic auth and a random password,
 * which would break all existing functionality. This permit-all config acts as
 * a bridge — it disables CSRF (since this is a stateless REST API) and allows
 * all requests through without authentication.
 *
 * This class will be progressively updated in later phases:
 * - Phase 3: Add PasswordEncoder bean (BCrypt)
 * - Phase 4: Add JWT filter, enforce authentication on all non-auth endpoints,
 *            configure stateless sessions, add CORS integration, and set up
 *            a custom JSON 401 error response
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
}
