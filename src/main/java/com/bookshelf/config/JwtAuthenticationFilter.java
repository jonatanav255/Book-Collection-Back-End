package com.bookshelf.config;

import com.bookshelf.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT authentication filter — runs on EVERY incoming HTTP request.
 *
 * This filter sits in the Spring Security filter chain and checks if the request
 * has a valid JWT access token in the Authorization header. If it does, the filter
 * sets up the SecurityContext so Spring Security knows the user is authenticated.
 *
 * How it works:
 * 1. Request comes in → filter checks for "Authorization: Bearer <token>" header
 * 2. If no header or not "Bearer " prefix → skip (let Spring Security handle as unauthenticated)
 * 3. If header exists → extract token → use JwtService to validate it
 * 4. If token is valid → create an Authentication object and set it in SecurityContext
 * 5. If token is invalid/expired → skip (request proceeds as unauthenticated → 401)
 *
 * extends OncePerRequestFilter: Guarantees this filter runs exactly ONCE per request,
 * even if the request is internally forwarded (e.g., error handling redirects).
 * Without this, the filter could run multiple times on the same request.
 *
 * @Component: Makes Spring auto-detect this class and manage it as a bean,
 * so it can be injected into SecurityConfig to register in the filter chain.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * Constructor injection — Spring provides the JwtService automatically.
     *
     * @param jwtService the service that validates JWT tokens and extracts usernames
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * The core filter logic — called for every HTTP request.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response (not used directly, but required by the filter contract)
     * @param filterChain the chain of remaining filters — we MUST call filterChain.doFilter()
     *                    to pass the request along, otherwise it gets stuck here
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: Get the Authorization header from the request
        // Example header: "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWI..."
        final String authHeader = request.getHeader("Authorization");

        // Step 2: If there's no Authorization header, or it doesn't start with "Bearer ",
        // skip this filter and let the request continue through the chain.
        // If the endpoint requires auth, Spring Security will return 401 later.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract the token by removing the "Bearer " prefix (7 characters)
        // "Bearer eyJhbGci..." → "eyJhbGci..."
        final String jwt = authHeader.substring(7);

        try {
            // Step 4: Extract the username from the token using JwtService
            // This also verifies the token's signature — if tampered with, it throws an exception
            final String username = jwtService.extractUsername(jwt);

            // Step 5: Only set authentication if:
            // - We successfully extracted a username from the token
            // - There's no authentication already set (avoid overwriting existing auth)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Step 6: Validate the token — checks signature AND expiration
                if (jwtService.isTokenValid(jwt, username)) {

                    // Step 7: Create a Spring Security authentication token
                    // This tells Spring Security "this user is authenticated"
                    // Parameters:
                    //   - principal (username): who is authenticated
                    //   - credentials (null): we don't need the password since JWT already proved identity
                    //   - authorities (empty list): we don't use roles in this app
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    username,               // The authenticated user's username
                                    null,                    // No credentials needed (JWT is the proof)
                                    Collections.emptyList()  // No roles/authorities (single-user, no RBAC)
                            );

                    // Step 8: Attach request details (IP address, session ID) to the auth token
                    // This is used for logging and auditing — not strictly required but good practice
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Step 9: Store the authentication in SecurityContext
                    // From this point on, Spring Security considers this request authenticated.
                    // Any controller can access the username via SecurityContextHolder.
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Step 10: If ANYTHING goes wrong (expired token, malformed token, bad signature),
            // we silently ignore it and let the request continue as unauthenticated.
            // Spring Security will then reject it with 401 if the endpoint requires auth.
            // We do NOT throw exceptions here — that would break the filter chain.
        }

        // Step 11: CRITICAL — always call filterChain.doFilter() to pass the request
        // to the next filter in the chain. If we don't call this, the request gets
        // stuck and the client never receives a response.
        filterChain.doFilter(request, response);
    }
}
