package com.moviebooking.paymentservice.client;

import com.moviebooking.paymentservice.client.dto.BookingValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
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
     * Fetch a booking to validate it exists, is PENDING, and belongs to this user.
     * Maps to: GET /api/bookings/{id} in BookingController.
     *
     * Called in initiatePayment() BEFORE creating a Payment record.
     * Prevents payments for:
     *   - Non-existent bookings (bookingId doesn't exist → 404 → clear error)
     *   - Already-confirmed bookings (status=CONFIRMED → reject with message)
     *   - Already-cancelled bookings (status=CANCELLED → reject with message)
     *   - Bookings belonging to another user (userId mismatch → 403)
     *
     * @param bookingId The booking to validate
     * @param userId    Forwarded so Booking Service can enforce ownership check
     * @param userRole  Forwarded so Booking Service allows admin access too
     */
    @GetMapping("/api/bookings/{id}")
    BookingValidationResponse getBooking(
            @PathVariable("id") Long bookingId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    );

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
