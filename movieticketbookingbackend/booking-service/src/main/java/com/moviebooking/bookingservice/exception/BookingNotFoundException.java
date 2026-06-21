package com.moviebooking.bookingservice.exception;

/**
 * Thrown when a booking ID is not found in booking_db.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(Long id) {
        super("Booking not found with ID: " + id);
    }
}
