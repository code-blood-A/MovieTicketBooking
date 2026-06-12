package com.moviebooking.discoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Discovery Server — Eureka Service Registry.
 *
 * ═══════════════════════════════════════════════════════════
 * WHAT DOES @EnableEurekaServer DO?
 * ═══════════════════════════════════════════════════════════
 * This single annotation activates the full Eureka Server machinery:
 *   1. Starts the service registry (in-memory registry of all services)
 *   2. Exposes REST endpoints that clients use to register/deregister
 *   3. Runs heartbeat monitoring (services send a ping every 30s;
 *      if 3 pings are missed → service is evicted from registry)
 *   4. Serves the Eureka dashboard UI at http://localhost:8761
 *
 * ═══════════════════════════════════════════════════════════
 * HOW DO OTHER SERVICES REGISTER?
 * ═══════════════════════════════════════════════════════════
 * Every other service (user-service, booking-service, etc.) will have:
 *   - spring-cloud-starter-netflix-eureka-client dependency
 *   - eureka.client.service-url.defaultZone pointing here
 *
 * On startup, they auto-register with:
 *   - Their spring.application.name (e.g., "user-service")
 *   - Their host:port
 *
 * When booking-service wants to call user-service via OpenFeign,
 * it asks Eureka: "Give me the current address of 'user-service'"
 * → Eureka returns the IP:port → Feign makes the HTTP call.
 * No hardcoded URLs anywhere.
 *
 * ═══════════════════════════════════════════════════════════
 * LLD PATTERN NOTE:
 * ═══════════════════════════════════════════════════════════
 * This is the Service Locator / Registry pattern at infrastructure level.
 * Services don't know WHERE other services are — they only know WHAT NAME
 * they have. The registry acts as the single source of truth for locations.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
