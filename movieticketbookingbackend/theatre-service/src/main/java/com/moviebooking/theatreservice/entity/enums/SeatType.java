package com.moviebooking.theatreservice.entity.enums;

/**
 * Type of seat — determines pricing and experience.
 *
 * WHY different seat types matter for the booking system:
 * - REGULAR  → cheapest, front/middle rows
 * - PREMIUM  → mid-priced, back rows with better view
 * - RECLINER → most expensive, full reclining seats
 *
 * The Booking Service uses seatType to calculate ticket price:
 *   REGULAR  × basePrice × 1.0
 *   PREMIUM  × basePrice × 1.5
 *   RECLINER × basePrice × 2.5
 *
 * Stored as STRING in DB (EnumType.STRING — safe for DB migrations).
 */
public enum SeatType {
    REGULAR,
    PREMIUM,
    RECLINER
}
