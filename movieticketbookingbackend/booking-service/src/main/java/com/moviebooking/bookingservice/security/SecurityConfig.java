package com.moviebooking.bookingservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — Booking Service security configuration.
 *
 * Permit-all pattern (same as Movie, Theatre, Show services).
 *
 * The API Gateway has already:
 *   1. Validated the JWT token.
 *   2. Rejected unauthenticated requests.
 *   3. Injected X-User-Id, X-User-Role, X-User-Email headers.
 *
 * Booking Service trusts these headers — no JWT parsing here.
 * The controller reads X-User-Id to identify who is making the request.
 *
 * CSRF disabled: stateless REST API — no session cookies, no CSRF risk.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
