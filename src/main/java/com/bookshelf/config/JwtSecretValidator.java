package com.bookshelf.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Validates the JWT secret key at application startup.
 *
 * This validator runs after the application is fully initialized and checks
 * whether the JWT secret is still set to the default development value.
 *
 * In development (default profile): logs a WARNING so developers know
 * they're using the dev key — acceptable for local testing.
 *
 * In production (any non-default profile, e.g., "prod", "production"):
 * FAILS THE APPLICATION STARTUP. Using the default dev key in production
 * would mean anyone who reads the source code could forge valid JWT tokens
 * and bypass authentication entirely.
 *
 * To fix in production, set the JWT_SECRET environment variable:
 *   export JWT_SECRET=$(openssl rand -base64 32)
 */
@Component
public class JwtSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretValidator.class);

    /** The default dev-only secret key — if this is still in use, we warn or fail */
    private static final String DEFAULT_DEV_SECRET =
            "dGhpcy1pcy1hLWRldi1vbmx5LWp3dC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhzMjU2";

    /** The configured JWT secret (from application.yml / JWT_SECRET env var) */
    @Value("${bookshelf.jwt.secret}")
    private String jwtSecret;

    /** The active Spring profiles (e.g., "default", "prod", "production") */
    @Value("${spring.profiles.active:default}")
    private String activeProfiles;

    /**
     * Runs after the application has fully started (all beans initialized, server ready).
     * Checks if the JWT secret is safe for the current environment.
     *
     * @EventListener(ApplicationReadyEvent.class) — Spring calls this method automatically
     * when the application is ready to serve requests.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateJwtSecret() {
        if (DEFAULT_DEV_SECRET.equals(jwtSecret)) {
            // Check if we're in a production-like profile
            if (isProductionProfile()) {
                // FAIL HARD — using the default key in production is a critical security risk
                log.error("==========================================================");
                log.error("  SECURITY ERROR: JWT secret is set to the default dev key!");
                log.error("  This is NOT safe for production.");
                log.error("  Set the JWT_SECRET environment variable to a secure value:");
                log.error("    export JWT_SECRET=$(openssl rand -base64 32)");
                log.error("==========================================================");
                throw new IllegalStateException(
                        "JWT secret must be changed from the default value in production. " +
                        "Set the JWT_SECRET environment variable."
                );
            } else {
                // In development, just warn — don't prevent startup
                log.warn("==========================================================");
                log.warn("  WARNING: Using default dev JWT secret key.");
                log.warn("  This is fine for local development, but MUST be changed");
                log.warn("  in production via the JWT_SECRET environment variable.");
                log.warn("==========================================================");
            }
        }
    }

    /**
     * Checks if any of the active Spring profiles indicate a production environment.
     * Returns true for profiles like "prod", "production", "staging", etc.
     *
     * @return true if the active profile suggests a production environment
     */
    private boolean isProductionProfile() {
        String profiles = activeProfiles.toLowerCase();
        return profiles.contains("prod") || profiles.contains("staging");
    }
}
