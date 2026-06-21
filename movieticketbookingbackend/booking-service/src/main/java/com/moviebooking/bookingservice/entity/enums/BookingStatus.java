package com.moviebooking.bookingservice.entity.enums;

/**
 * BookingStatus — lifecycle states of a Booking.
 *
 * State machine:
 * ──────────────
 *
 *   [User selects seats]
 *          ↓
 *       PENDING ──── (payment failed / user cancelled) ──→ CANCELLED
 *          │
 *          │ (payment success)
 *          ↓
 *      CONFIRMED
 *
 *       PENDING ──── (unexpected error) ──→ FAILED
 *
 * WHY keep PENDING state?
 * ─────────────────────────
 * When a booking is created:
 *  1. Seats are LOCKED in Show Service
 *  2. Booking is created in PENDING state
 *  3. User is redirected to Payment Service
 *
 * Between step 2 and step 3, the booking is PENDING.
 * If the user closes the browser or payment times out,
 * a scheduled cleanup job can find all PENDING bookings
 * older than 10 minutes and cancel them (releasing seat locks).
 *
 * WHY FAILED vs CANCELLED?
 * ─────────────────────────
 * CANCELLED = user intentionally cancelled (or admin cancelled).
 * FAILED = system error during booking creation or confirmation
 *          (e.g., Show Service was down when we tried to BOOK seats).
 * Keeping these separate helps with analytics and support queries.
 */
public enum BookingStatus {

    /**
     * Seats are LOCKED, payment not yet completed.
     * Booking is in progress — waiting for payment confirmation.
     */
    PENDING,

    /**
     * Payment successful. Seats are BOOKED.
     * This is the happy path final state.
     */
    CONFIRMED,

    /**
     * User cancelled or payment abandoned.
     * Seat locks are released back to AVAILABLE.
     */
    CANCELLED,

    /**
     * System error during booking flow.
     * E.g., Show Service unavailable when trying to mark seats BOOKED.
     * Seat locks may need manual investigation/release.
     */
    FAILED
}
