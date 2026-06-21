package com.moviebooking.showservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — Show Service security configuration.
 *
 * DESIGN DECISION: Permit all requests here.
 *
 * WHY permit all? Isn't this insecure?
 * ──────────────────────────────────────
 * The API Gateway is the single entry point for all traffic.
 * The Gateway's JwtAuthenticationFilter already:
 *   1. Validates JWT tokens
 *   2. Rejects invalid/missing tokens for protected paths
 *   3. Injects X-User-Id, X-User-Role, X-User-Email headers
 *
 * By the time a request reaches Show Service, the Gateway has already
 * authenticated and authorized it. The Show Service trusts these headers.
 *
 * Admin check in controller: "ROLE_ADMIN".equals(request.getHeader("X-User-Role"))
 * This is simpler and avoids duplicating JWT validation in every service.
 *
 * In a zero-trust production setup, you'd also add mTLS between services
 * so only the Gateway can call Show Service (not arbitrary external callers).
 * For this learning project, Gateway-level security is sufficient.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF: stateless REST APIs don't need CSRF protection.
            // CSRF protects against form-based attacks in session-based apps.
            // JWT is stateless — no session, no CSRF risk.
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session: no HttpSession created or used.
            // Every request is self-contained (validated by Gateway's JWT check).
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Permit everything — Gateway has already done the auth work.
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
