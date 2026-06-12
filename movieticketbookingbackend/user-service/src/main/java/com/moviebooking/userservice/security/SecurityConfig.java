package com.moviebooking.userservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration for User Service.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY PERMIT ALL if we have JWT auth in the Gateway?
 * ═══════════════════════════════════════════════════════════
 * In our architecture, the API Gateway is the SINGLE security boundary.
 * It validates JWT before any request reaches user-service.
 *
 * So user-service itself doing JWT validation would be REDUNDANT — the
 * Gateway already ensured only valid requests come through.
 *
 * Strategy: "Trusted Network" model
 * - External traffic → Gateway (validates JWT) → internal services
 * - Internal services trust requests arriving from the Gateway
 * - The X-User-Id header IS the identity (Gateway vouches for it)
 *
 * In production, this is enforced at the network level (Kubernetes NetworkPolicy,
 * or a service mesh like Istio) — user-service is unreachable except via Gateway.
 * For local development, we simply permit all requests (no real attack surface).
 *
 * ═══════════════════════════════════════════════════════════
 * WHY KEEP Spring Security at all then?
 * ═══════════════════════════════════════════════════════════
 * We NEED it for:
 * 1. BCryptPasswordEncoder bean — used in UserServiceImpl to hash/verify passwords
 *    (BCryptPasswordEncoder is a Spring Security class)
 * 2. Future: If you add method-level security (@PreAuthorize), Spring Security
 *    must be present and configured
 * 3. Defense in depth: Even permitting all, Spring Security still protects against
 *    some common web vulnerabilities (headers like X-Content-Type-Options, etc.)
 *
 * ═══════════════════════════════════════════════════════════
 * STATELESS SESSION — CRITICAL
 * ═══════════════════════════════════════════════════════════
 * SessionCreationPolicy.STATELESS tells Spring Security:
 * "NEVER create an HttpSession for authentication."
 *
 * WHY? In microservices, each request is stateless (carries its own JWT).
 * If Spring Security creates sessions, you'd need session replication across
 * service instances — that's a distributed state nightmare. Stateless = scalable.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF: we're stateless (no cookies, no sessions) so CSRF doesn't apply.
            // CSRF attacks work by exploiting browser cookie auto-send. JWT in Authorization
            // header is NOT auto-sent by browsers → no CSRF risk.
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless: no HttpSession created or used
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Permit all: Gateway is the gatekeeper. Trust the network.
            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * BCryptPasswordEncoder Bean.
     *
     * Strength parameter (default=10): the "work factor".
     * BCrypt performs 2^10 = 1024 iterations of hashing.
     * Increasing to 12 → 2^12 = 4096 iterations (4x slower → harder to brute force).
     *
     * WHY @Bean? So Spring can inject PasswordEncoder wherever it's needed
     * (UserServiceImpl uses it for hashing and verification).
     * It's defined here (not in UserServiceImpl) to avoid circular dependencies
     * and keep the security config centralized.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
