package com.bookshelf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsFilterConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsFilterConfig.class);

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Read directly from System.getenv to bypass any Spring property resolution issues
        String envValue = System.getenv("CORS_ORIGINS");
        String origins = envValue != null ? envValue : "http://localhost:3000";

        log.info("=== CORS CONFIG === System.getenv('CORS_ORIGINS'): '{}'", envValue);
        log.info("=== CORS CONFIG === Using origins: '{}'", origins);

        CorsConfiguration config = new CorsConfiguration();
        List<String> originList = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        log.info("=== CORS CONFIG === Parsed origin list: {}", originList);

        config.setAllowedOrigins(originList);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
