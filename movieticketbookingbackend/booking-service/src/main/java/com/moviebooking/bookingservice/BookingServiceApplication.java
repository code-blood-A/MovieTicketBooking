package com.moviebooking.bookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * BookingServiceApplication — entry point for Booking Service.
 *
 * Key annotations:
 * ─────────────────
 * @SpringBootApplication:
 *   Composite of @Configuration + @EnableAutoConfiguration + @ComponentScan.
 *   Auto-configures JPA, web, security, Eureka etc. based on classpath.
 *
 * @EnableFeignClients:
 *   CRITICAL — without this, @FeignClient annotated interfaces are NOT
 *   registered as Spring beans. The app will start but injection will fail.
 *
 *   This annotation tells Spring to:
 *   1. Scan for all interfaces annotated with @FeignClient in this package.
 *   2. Generate a dynamic proxy implementation for each one.
 *   3. Register them as beans so @Autowired / constructor injection works.
 *
 *   The proxy uses the Feign library to translate interface method calls
 *   into actual HTTP requests (GET/POST/PUT etc.) sent over the network.
 *
 * Virtual Threads (Java 21 Project Loom):
 *   Configured in application.yml (spring.threads.virtual.enabled=true).
 *   Each HTTP request (from client) + each Feign call (to Show Service)
 *   runs on a lightweight virtual thread instead of a platform thread.
 *   This dramatically increases throughput for I/O-heavy booking flows
 *   without needing reactive programming (WebFlux).
 */
@SpringBootApplication
@EnableFeignClients
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
