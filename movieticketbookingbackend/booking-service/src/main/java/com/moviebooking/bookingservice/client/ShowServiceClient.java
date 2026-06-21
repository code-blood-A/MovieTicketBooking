package com.moviebooking.bookingservice.client;

import com.moviebooking.bookingservice.dto.ShowSeatResponse;
import com.moviebooking.bookingservice.dto.ShowSeatStatusRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * ShowServiceClient — Feign Client for calling Show Service.
 *
 * ═══════════════════════════════════════════════════════
 * HOW FEIGN CLIENT WORKS (Important Learning Concept)
 * ═══════════════════════════════════════════════════════
 *
 * 1. You write an interface (this file).
 * 2. At startup, @EnableFeignClients (in BookingServiceApplication)
 *    triggers Spring to scan for @FeignClient interfaces.
 * 3. Spring generates a runtime proxy that implements this interface.
 * 4. When you call showServiceClient.updateSeatStatus(...),
 *    the proxy translates it into:
 *      - Build HTTP PUT request
 *      - URL: http://<show-service-host>/api/shows/{showId}/seats/{seatId}/status
 *      - Add X-User-Id header
 *      - Serialize request body to JSON
 *      - Send over network
 *      - Deserialize JSON response into ShowSeatResponse
 * 5. Eureka + Load Balancer resolve "show-service" to the actual IP:port.
 *
 * @FeignClient(name = "show-service"):
 *   "show-service" is the spring.application.name from Show Service's
 *   application.yml. Eureka uses this name as the service registry key.
 *   The load balancer (Spring Cloud LoadBalancer) picks one instance
 *   if multiple replicas are running.
 *
 * ═══════════════════════════════════════════════════════
 * SERVICE-TO-SERVICE AUTHENTICATION
 * ═══════════════════════════════════════════════════════
 * Booking Service calls Show Service DIRECTLY (bypassing Gateway).
 * Show Service has SecurityConfig: permitAll() → no JWT needed.
 *
 * But we still pass X-User-Id because Show Service's updateSeatStatus
 * uses it to set lockedByUserId on the ShowSeat (for audit + validation).
 *
 * The userId comes from the X-User-Id header that Gateway injected
 * into the original request from the client. Booking Service reads it
 * and forwards it to Show Service.
 *
 * In production (zero-trust): add mTLS or an internal API key so only
 * Booking Service can call Show Service's seat mutation endpoints.
 */
@FeignClient(name = "show-service")
public interface ShowServiceClient {

    /**
     * Updates the status of a specific seat in a show.
     *
     * Called in three scenarios:
     * 1. LOCK  → when user creates a booking (seats reserved)
     * 2. BOOK  → when payment is confirmed (seats permanently booked)
     * 3. AVAILABLE → when booking is cancelled (seats released)
     *
     * @param showId  The show's ID
     * @param seatId  The ShowSeat's primary key (not Theatre's seat ID)
     * @param request Body with new status and userId
     * @param userId  Forwarded from Booking Service's incoming request header
     * @return Updated ShowSeatResponse with confirmed new status + price info
     */
    @PutMapping("/api/shows/{showId}/seats/{seatId}/status")
    ShowSeatResponse updateSeatStatus(
            @PathVariable("showId") Long showId,
            @PathVariable("seatId") Long seatId,
            @RequestBody ShowSeatStatusRequest request,
            @RequestHeader("X-User-Id") Long userId
    );
}
