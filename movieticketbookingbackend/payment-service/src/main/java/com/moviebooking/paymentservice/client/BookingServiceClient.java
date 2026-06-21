package com.moviebooking.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * BookingServiceClient — Feign Client for calling Booking Service.
 *
 * Payment Service calls Booking Service to:
 *   1. Confirm a booking after successful payment → seats become BOOKED.
 *   2. Cancel a booking after failed payment → seats released to AVAILABLE.
 *
 * @FeignClient(name = "booking-service"):
 *   "booking-service" = spring.application.name in Booking Service's application.yml.
 *   Eureka resolves this to the actual IP:port of the Booking Service instance.
 *   No hardcoded URLs — if Booking Service moves to a different port/host,
 *   this code doesn't need to change.
 *
 * Return type is void:
 *   We don't need the BookingResponse here — Payment Service only cares
 *   that the confirm/cancel call succeeded (no FeignException thrown).
 *   If it fails, we catch FeignException in PaymentServiceImpl and
 *   handle the error gracefully.
 *
 * X-User-Id header:
 *   Booking Service's confirm/cancel endpoints read X-User-Id to verify
 *   the caller is the booking's owner.
 *   Payment Service forwards the userId it received from the Gateway.
 */
@FeignClient(name = "booking-service")
public interface BookingServiceClient {

    /**
     * Confirm a booking after successful payment.
     * Maps to: POST /api/bookings/{id}/confirm in BookingController.
     *
     * @param bookingId The booking to confirm
     * @param userId    The paying user's ID (forwarded from Gateway header)
     */
    @PostMapping("/api/bookings/{id}/confirm")
    void confirmBooking(
            @PathVariable("id") Long bookingId,
            @RequestHeader("X-User-Id") Long userId
    );

    /**
     * Cancel a booking after failed/declined payment.
     * Maps to: POST /api/bookings/{id}/cancel in BookingController.
     * Booking Service will release all LOCKED seat back to AVAILABLE.
     *
     * @param bookingId The booking to cancel
     * @param userId    The paying user's ID (forwarded from Gateway header)
     */
    @PostMapping("/api/bookings/{id}/cancel")
    void cancelBooking(
            @PathVariable("id") Long bookingId,
            @RequestHeader("X-User-Id") Long userId
    );
}
