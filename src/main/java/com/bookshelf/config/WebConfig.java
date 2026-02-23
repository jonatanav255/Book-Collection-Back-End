package com.bookshelf.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for CORS and MVC settings
 * Allows frontend to access backend from different origin (localhost:3000 → localhost:8080)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure CORS to allow all origins for API endpoints
     * Permits GET, POST, PUT, DELETE, OPTIONS methods
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://books.jonathan-dev.com", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
