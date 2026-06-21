package com.moviebooking.bookingservice.exception;

/**
 * Thrown when one or more seats could not be locked during booking creation.
 *
 * This happens when:
 *   - A seat is already LOCKED by another user currently in checkout.
 *   - A seat is already BOOKED (sold out).
 *
 * The compensation action (releasing previously locked seats) has already
 * been performed before this exception is thrown.
 *
 * HTTP status: 409 Conflict (handled by GlobalExceptionHandler)
 * — "Conflict" because the seat state conflicts with the user's request.
 */
public class SeatLockException extends RuntimeException {

    public SeatLockException(String message) {
        super(message);
    }
}
