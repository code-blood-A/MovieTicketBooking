package com.moviebooking.showservice.dto;

import com.moviebooking.showservice.entity.enums.ShowSeatStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * ShowSeatResponse — the seat map item returned to clients.
 *
 * Returned as a list from GET /api/shows/{showId}/seats
 * The frontend uses this data to render the interactive seat selection UI:
 *
 * Row A: [A1:AVAILABLE] [A2:AVAILABLE] [A3:BOOKED] ...
 * Row B: [B1:AVAILABLE] [B2:LOCKED]   [B3:AVAILABLE] ...
 * ...
 * Row J: [J1:AVAILABLE] [J2:AVAILABLE] ...  ← RECLINER (price × 2.0)
 *
 * Color coding in UI:
 *   AVAILABLE → green  (clickable)
 *   LOCKED    → grey   (non-clickable, "someone else is paying for this")
 *   BOOKED    → red    (non-clickable, "sold out")
 */
@Getter
@Builder
public class ShowSeatResponse {
    private Long id;          // ShowSeat ID — used by Booking Service to lock/book
    private Long seatId;      // Physical seat ID from Theatre Service
    private String seatLabel; // e.g., "E5", "J12" — for UI display
    private String seatType;  // REGULAR, PREMIUM, RECLINER — for price badge
    private BigDecimal price; // Final price for this specific seat
    private ShowSeatStatus status; // AVAILABLE, LOCKED, BOOKED
}
