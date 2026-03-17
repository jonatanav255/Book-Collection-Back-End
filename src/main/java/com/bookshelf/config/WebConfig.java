package com.bookshelf.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for CORS and MVC settings.
 * Allows frontend to access backend from different origin (localhost:3000 → localhost:8080).
 *
 * CORS (Cross-Origin Resource Sharing) is a browser security feature that blocks
 * requests from one origin (localhost:3000) to another (localhost:8080) by default.
 * This config tells the browser: "requests from these origins are allowed."
 *
 * Spring Security delegates to this CORS config via Customizer.withDefaults()
 * in SecurityConfig. This means the CORS rules defined here apply at the
 * security filter level too, so preflight OPTIONS requests are not blocked.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${bookshelf.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Configure CORS to allow specified origins for API endpoints.
     * Permits GET, POST, PUT, DELETE, OPTIONS methods.
     *
     * exposedHeaders("Authorization") — allows the frontend JavaScript to READ
     * the Authorization header from responses. Without this, the browser hides
     * the header from JavaScript even if the server sends it. This is needed
     * so the frontend can read token-related headers if the backend ever sends them.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization")
                .maxAge(3600);
    }
}
