package com.moviebooking.discoveryserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration for the Eureka Server.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY DOES EUREKA NEED SPECIAL SECURITY CONFIG?
 * ═══════════════════════════════════════════════════════════
 * Spring Security (added to protect the dashboard) enables CSRF protection
 * by default. CSRF (Cross-Site Request Forgery) protection works by requiring
 * a token in every state-changing request (POST, PUT, DELETE).
 *
 * The problem: Eureka client services (user-service, booking-service, etc.)
 * register themselves by POSTing to /eureka/** on startup. These are
 * machine-to-machine calls — they don't carry a CSRF token. Result:
 * every service registration FAILS with 403 Forbidden.
 *
 * THE FIX: Disable CSRF specifically for /eureka/** paths.
 * We keep CSRF active for everything else (the web dashboard).
 *
 * ═══════════════════════════════════════════════════════════
 * SPRING BOOT 3.x SECURITY API (Lambda DSL)
 * ═══════════════════════════════════════════════════════════
 * Spring Boot 3.x deprecated the old chained .and() style:
 *   http.csrf().disable()  ← OLD, deprecated in Spring Security 6
 *
 * The new Lambda DSL style is:
 *   http.csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
 *
 * This is more readable and composable. Get used to this pattern —
 * you'll see it in API Gateway and User Service too.
 *
 * ═══════════════════════════════════════════════════════════
 * LLD PATTERN: This is the Builder Pattern
 * ═══════════════════════════════════════════════════════════
 * HttpSecurity is a classic Builder — you chain configuration calls
 * and call .build() (implicitly via http.build()) to produce the
 * final SecurityFilterChain object.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Defines the security rules as a filter chain.
     *
     * SecurityFilterChain is Spring Security's way of saying:
     * "For HTTP requests matching these rules, apply these filters in order."
     *
     * Every incoming request passes through this chain before hitting any controller.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF only for Eureka registration endpoints.
            // Eureka clients (other microservices) POST here to register —
            // they don't use browser sessions so they can't carry CSRF tokens.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/eureka/**")
            )

            // All requests must be authenticated (username + password via Basic Auth).
            // Credentials are defined in application.yml under spring.security.user.*
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )

            // Enable HTTP Basic Authentication.
            // This is how microservices authenticate when registering:
            // they embed credentials in the eureka.client.service-url.defaultZone URL
            // as: http://username:password@localhost:8761/eureka/
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
