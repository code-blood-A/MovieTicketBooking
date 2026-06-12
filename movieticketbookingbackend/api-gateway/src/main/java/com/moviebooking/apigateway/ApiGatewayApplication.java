package com.moviebooking.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway — Single Entry Point for all Microservices.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY NO @EnableEurekaClient HERE?
 * ═══════════════════════════════════════════════════════════
 * With Spring Cloud 2021+, you do NOT need @EnableEurekaClient or
 * @EnableDiscoveryClient annotations. Simply having the Eureka client
 * dependency on the classpath + the eureka config in application.yml
 * is enough — Spring Boot auto-configuration handles everything.
 *
 * @EnableEurekaClient is still valid but redundant. Removing it keeps
 * the code clean and follows the modern Spring Boot convention:
 * "convention over configuration" — if the jar is there, it works.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY NO @EnableWebMvc OR @EnableWebFlux?
 * ═══════════════════════════════════════════════════════════
 * Spring Boot auto-detects the presence of spring-cloud-starter-gateway
 * and sets up the WebFlux context automatically. Adding @EnableWebMvc
 * would switch it to Servlet mode and BREAK the gateway. Don't add it.
 *
 * ═══════════════════════════════════════════════════════════
 * WHAT THIS SERVICE DOES:
 * ═══════════════════════════════════════════════════════════
 * 1. Receives ALL HTTP requests from clients (port 8080)
 * 2. JwtAuthenticationFilter runs first:
 *    - Public routes (login, register) → pass through immediately
 *    - Protected routes → validate JWT from Authorization header
 *    - Invalid/missing JWT → 401 Unauthorized (request dies here)
 *    - Valid JWT → extract userId, role → add as X-User-Id, X-User-Role headers
 * 3. Route matching (defined in application.yml):
 *    - /api/users/**    → lb://user-service
 *    - /api/movies/**   → lb://movie-service
 *    - /api/bookings/** → lb://booking-service
 *    - etc.
 * 4. lb:// prefix → Spring Cloud LoadBalancer asks Eureka for the
 *    actual IP:port of the target service → forwards the request
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
