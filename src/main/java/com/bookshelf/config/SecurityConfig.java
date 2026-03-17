package com.bookshelf.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration — the central place that controls WHO can access WHAT.
 *
 * This class configures the security filter chain, which is a series of filters that
 * every HTTP request passes through before reaching a controller. Think of it as a
 * series of checkpoints at an airport — each filter inspects the request and decides
 * whether to let it through.
 *
 * What this configuration does:
 * 1. Disables CSRF (not needed for stateless JWT APIs)
 * 2. Enables CORS (delegates to WebConfig's CORS settings)
 * 3. Sets session management to STATELESS (no server-side sessions, JWT handles state)
 * 4. Defines which endpoints are public vs protected:
 *    - /api/auth/** → public (login, register, refresh, logout)
 *    - Swagger/OpenAPI docs → public (API documentation)
 *    - Everything else → requires a valid JWT token
 * 5. Registers the JwtAuthenticationFilter BEFORE Spring's default auth filter
 * 6. Returns JSON error responses for 401 (instead of Spring's default HTML page)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * The JWT filter we created — inspects every request for a Bearer token.
     * Injected by Spring because JwtAuthenticationFilter is annotated with @Component.
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${bookshelf.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configures the security filter chain — the heart of Spring Security.
     *
     * Every HTTP request goes through this chain in order:
     * CORS filter → CSRF check → JWT filter → authorization check → controller
     *
     * @param http the HttpSecurity builder used to configure security rules
     * @return the fully configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── CSRF ──────────────────────────────────────────────────────
                // Disable CSRF protection. CSRF attacks exploit cookie-based auth,
                // but we use JWT tokens in the Authorization header, which browsers
                // don't automatically attach to cross-origin requests. So CSRF
                // attacks are not possible with our auth approach.
                .csrf(csrf -> csrf.disable())

                // ── CORS ──────────────────────────────────────────────────────
                // Enable CORS and delegate to WebConfig's addCorsMappings() method.
                // Customizer.withDefaults() tells Spring Security: "use the CORS config
                // that's already defined elsewhere (WebConfig)" instead of blocking
                // cross-origin requests at the security filter level.
                // Without this, Spring Security would reject preflight OPTIONS requests
                // from the frontend (localhost:3000) before they even reach WebConfig.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── SESSION MANAGEMENT ────────────────────────────────────────
                // STATELESS means Spring Security will NOT create or use HTTP sessions.
                // In traditional web apps, the server stores a session cookie to track
                // logged-in users. With JWT, the token itself carries all the info,
                // so server-side sessions are unnecessary and wasteful.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ── AUTHORIZATION RULES ───────────────────────────────────────
                // Define which endpoints are public and which require authentication.
                // Rules are evaluated in ORDER — first match wins.
                .authorizeHttpRequests(auth -> auth
                        // Health check endpoint is public — used by Railway deployment healthcheck
                        .requestMatchers("/api/health").permitAll()

                        // Auth endpoints are public — you can't require a token to get a token!
                        .requestMatchers("/api/auth/**").permitAll()

                        // Swagger/OpenAPI docs are public — accessible without login
                        // so developers can browse the API documentation
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // EVERYTHING ELSE requires authentication.
                        // If a request reaches here without a valid JWT token in the
                        // SecurityContext (set by JwtAuthenticationFilter), Spring Security
                        // will reject it and trigger the authenticationEntryPoint below.
                        .anyRequest().authenticated()
                )

                // ── CUSTOM 401 ERROR RESPONSE ─────────────────────────────────
                // By default, Spring Security returns an HTML error page when a request
                // is rejected (401 Unauthorized). Since this is a REST API consumed by
                // a frontend, we need a JSON response instead.
                // This handler fires when an unauthenticated request hits a protected endpoint.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}"
                            );
                        })
                )

                // ── JWT FILTER REGISTRATION ───────────────────────────────────
                // Insert our JwtAuthenticationFilter BEFORE Spring's built-in
                // UsernamePasswordAuthenticationFilter.
                //
                // The filter chain order becomes:
                // ... → CORS → CSRF → JwtAuthenticationFilter → UsernamePasswordAuthFilter → ...
                //
                // Our filter runs first to extract and validate the JWT token.
                // If valid, it sets the Authentication in SecurityContext.
                // Then when the authorization check runs (.anyRequest().authenticated()),
                // it sees the authentication is set and allows the request through.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
