package com.moviebooking.bookingservice.controller;

import com.moviebooking.bookingservice.dto.BookingRequest;
import com.moviebooking.bookingservice.dto.BookingResponse;
import com.moviebooking.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BookingController — REST API for Booking operations.
 *
 * Endpoint summary:
 * ┌──────────────────────────────────────────────────────────────────┐
 * │ AUTHENTICATED (JWT required — Gateway enforces this) │
 * │ POST /api/bookings — create a new booking │
 * │ GET /api/bookings/my — get my bookings │
 * │ GET /api/bookings/{id} — get booking by ID │
 * │ POST /api/bookings/{id}/confirm — confirm (simulate payment OK) │
 * │ POST /api/bookings/{id}/cancel — cancel booking │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * All endpoints read userId from X-User-Id header (injected by Gateway).
 * No userId in request body — prevents impersonation attacks.
 *
 * Admin check:
 * GET /api/bookings/{id} also allows admins (X-User-Role = ROLE_ADMIN)
 * to view any booking, not just their own.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    /**
     * POST /api/bookings
     * Create a booking: locks seats and returns a PENDING booking.
     *
     * Request body: { "showId": 1, "seatIds": [1, 2, 3] }
     *
     * The client should:
     * 1. Call this endpoint to create PENDING booking + lock seats.
     * 2. Show payment UI to the user.
     * 3. On payment success → call POST /api/bookings/{id}/confirm.
     * 4. On payment failure/cancel → call POST /api/bookings/{id}/cancel.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Received booking request: userId={}, showId={}, seats={}",
                userId, request.getShowId(), request.getSeatIds());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, userId));
    }

    /**
     * GET /api/bookings/my
     * Returns all bookings for the authenticated user.
     * Sorted newest first.
     *
     * NOTE: This endpoint MUST be defined BEFORE /{id} to avoid
     * Spring routing "my" as a Long path variable and getting a
     * NumberFormatException on the /api/bookings/my URL.
     */
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bookingService.getMyBookings(userId));
    }

    /**
     * GET /api/bookings/{id}
     * Get a booking by its ID.
     * Only the booking owner or an admin can view it.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long userId,
            @RequestHeader(name = "X-User-Role", required = false) String userRole) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BookingResponse booking = bookingService.getBookingById(id);

        // Authorization: only the owner or an admin can view this booking
        boolean isOwner = booking.getUserId().equals(userId);
        boolean isAdmin = "ROLE_ADMIN".equals(userRole);

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(booking);
    }

    /**
     * POST /api/bookings/{id}/confirm
     * Simulates payment success.
     *
     * In production flow:
     * 1. Client calls POST /api/bookings → gets PENDING booking.
     * 2. Client sends user to payment page (Razorpay/Stripe).
     * 3. Payment gateway sends webhook to Payment Service.
     * 4. Payment Service calls this endpoint (or publishes Kafka event).
     *
     * For MVP:
     * 1. Client calls POST /api/bookings → PENDING booking.
     * 2. Client calls POST /api/bookings/{id}/confirm → CONFIRMED.
     * (No actual payment involved — we simulate it.)
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bookingService.confirmBooking(id, userId));
    }

    /**
     * POST /api/bookings/{id}/cancel
     * Cancel a PENDING booking and release all seat locks.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bookingService.cancelBooking(id, userId));
    }
}
