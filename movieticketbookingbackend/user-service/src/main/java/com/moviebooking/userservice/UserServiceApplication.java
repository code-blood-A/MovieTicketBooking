package com.moviebooking.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * User Service — Owns registration, login, JWT issuance, and user profiles.
 *
 * ═══════════════════════════════════════════════════════════
 * VIRTUAL THREADS (Java 21 — Project Loom)
 * ═══════════════════════════════════════════════════════════
 * We enable virtual threads in application.yml:
 *   spring.threads.virtual.enabled: true
 *
 * What this does: Every incoming HTTP request that would normally be handled
 * by a Tomcat platform thread is now handled by a virtual thread.
 *
 * WHY it matters for this service:
 *   - User login hits the DB (blocking JDBC call) to fetch the user
 *   - Then it does BCrypt verification (CPU-bound, ~200ms)
 *   - With platform threads: thread is blocked during DB wait → wastes resources
 *   - With virtual threads: JVM parks the virtual thread during DB wait,
 *     the carrier thread picks up other work → much better utilization
 *
 * Result: Same blocking code you already know, but scales like async code.
 * This is Java 21's gift — you don't need to learn reactive programming
 * for standard microservices. Only the Gateway needs reactive (WebFlux).
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
