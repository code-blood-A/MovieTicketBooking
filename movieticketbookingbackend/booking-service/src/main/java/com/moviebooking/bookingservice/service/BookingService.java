package com.moviebooking.bookingservice.service;

import com.moviebooking.bookingservice.dto.BookingRequest;
import com.moviebooking.bookingservice.dto.BookingResponse;

import java.util.List;

/**
 * BookingService — interface defining all booking operations.
 *
 * WHY use an interface?
 * ─────────────────────
 * 1. Dependency Inversion Principle (SOLID-D):
 *    BookingController depends on BookingService (interface),
 *    not on BookingServiceImpl (concrete class).
 *    If we swap the implementation (e.g., async version), controller unchanged.
 *
 * 2. Spring's @Transactional proxy works on interfaces by default.
 *    Spring wraps the impl with a proxy bean that manages transactions.
 *
 * 3. Testability: In unit tests, we can mock BookingService without
 *    instantiating the full impl (no DB, no Feign calls needed).
 */
public interface BookingService {

    /**
     * Create a new booking:
     * 1. Lock all requested seats via Show Service (Feign call).
     * 2. On partial failure: release any already-locked seats.
     * 3. Persist Booking (PENDING) + BookingItems.
     * 4. Return booking summary with PENDING status.
     *
     * @param request  Seats + showId from client
     * @param userId   From X-User-Id header (Gateway-injected)
     * @return PENDING booking with all seat details and total
     */
    BookingResponse createBooking(BookingRequest request, Long userId);

    /**
     * Get a booking by its ID.
     * Access control (owner-only or admin) is enforced in the controller.
     */
    BookingResponse getBookingById(Long bookingId);

    /**
     * Get all bookings for the authenticated user.
     *
     * @param userId From X-User-Id header
     */
    List<BookingResponse> getMyBookings(Long userId);

    /**
     * Confirm a PENDING booking after successful payment.
     * Calls Show Service to mark all seats as BOOKED.
     * Updates booking status to CONFIRMED.
     *
     * In a production system, this would be called by Payment Service
     * via a webhook/event after payment gateway confirmation.
     * For MVP, the client calls this directly to simulate payment.
     *
     * @param bookingId Booking to confirm
     * @param userId    Must match booking's userId (owner check)
     */
    BookingResponse confirmBooking(Long bookingId, Long userId);

    /**
     * Cancel a booking.
     * Calls Show Service to release all LOCKED seats back to AVAILABLE.
     * Updates booking status to CANCELLED.
     *
     * @param bookingId Booking to cancel
     * @param userId    Must match booking's userId (or be ROLE_ADMIN)
     */
    BookingResponse cancelBooking(Long bookingId, Long userId);
}
