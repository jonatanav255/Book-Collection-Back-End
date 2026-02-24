package com.bookshelf.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for CORS and MVC settings
 * Allows frontend to access backend from different origin (localhost:3000 → localhost:8080)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${bookshelf.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Configure CORS to allow specified origins for API endpoints
     * Permits GET, POST, PUT, DELETE, OPTIONS methods
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
