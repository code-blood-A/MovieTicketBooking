package com.moviebooking.bookingservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * BookingItemResponse — per-seat detail in a booking response.
 *
 * Returned as part of BookingResponse.items[].
 * Contains all information needed to render a ticket line item:
 *   "Seat E5 | PREMIUM | ₹225"
 */
@Getter
@Builder
public class BookingItemResponse {

    private Long id;

    /** ShowSeat's PK in show_db — client can use this for further seat queries */
    private Long showSeatId;

    /** Human-readable seat identifier (e.g., "E5", "J12") */
    private String seatLabel;

    /** REGULAR, PREMIUM, or RECLINER */
    private String seatType;

    /** Price paid for this seat at booking time (snapshot) */
    private BigDecimal price;
}
