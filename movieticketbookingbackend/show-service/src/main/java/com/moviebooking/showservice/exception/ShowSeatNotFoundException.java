package com.moviebooking.showservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a ShowSeat cannot be found for a given show + seat combination.
 * Maps to HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ShowSeatNotFoundException extends RuntimeException {
    public ShowSeatNotFoundException(Long showId, Long seatId) {
        super("Seat not found: seatId=" + seatId + " in showId=" + showId);
    }
}
