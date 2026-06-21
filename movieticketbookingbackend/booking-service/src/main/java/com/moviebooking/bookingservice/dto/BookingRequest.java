package com.moviebooking.bookingservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * BookingRequest — what the client sends to create a booking.
 *
 * Minimal contract:
 * - Which show? → showId
 * - Which seats? → seatIds (ShowSeat primary keys from show_db)
 *
 * NOT in the request:
 * - userId → read from X-User-Id header (injected by Gateway from JWT)
 * - price → fetched from Show Service's seat response
 * - status → always starts as PENDING
 *
 * This keeps the API clean — the client doesn't need to know prices
 * (prevent price manipulation from client side).
 */
@Getter
@Setter
public class BookingRequest {

    /**
     * The show to book seats in.
     * Must match an existing show in show_db.
     */
    @NotNull(message = "Show ID is required")
    private Long showId;

    /**
     * List of ShowSeat IDs (PKs from show_seats table).
     * The client gets these IDs from GET /api/shows/{showId}/seats.
     * They represent specific seats: "seat 42 = row E, seat 5, PREMIUM".
     *
     * @NotEmpty: must have at least one seat — can't book an empty cart.
     */
    @NotNull(message = "Seat IDs are required")
    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> seatIds;
}
