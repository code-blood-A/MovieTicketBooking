package com.moviebooking.bookingservice.dto;

import com.moviebooking.bookingservice.entity.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BookingResponse — full booking summary returned to the client.
 *
 * Returned by:
 *   POST /api/bookings       → after creating a booking (PENDING)
 *   GET  /api/bookings/{id}  → fetch booking details
 *   POST /api/bookings/{id}/confirm → after confirming (CONFIRMED)
 *   POST /api/bookings/{id}/cancel  → after cancelling (CANCELLED)
 */
@Getter
@Builder
public class BookingResponse {

    private Long id;

    /** The user who made this booking */
    private Long userId;

    /** The show this booking is for */
    private Long showId;

    /** Current status: PENDING, CONFIRMED, CANCELLED, FAILED */
    private BookingStatus status;

    /** Total amount (sum of all item prices) */
    private BigDecimal totalAmount;

    /**
     * All seats in this booking.
     * Each item shows: seatLabel, seatType, price.
     * Used to render the booking summary / ticket.
     */
    @Builder.Default
    private List<BookingItemResponse> items = new java.util.ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
