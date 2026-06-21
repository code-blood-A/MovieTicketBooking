package com.moviebooking.showservice.entity.enums;

/**
 * ShowStatus — lifecycle state of a scheduled show.
 *
 * State transitions:
 *   SCHEDULED → OPEN_FOR_BOOKING → ONGOING → COMPLETED
 *                    ↓
 *               CANCELLED (admin can cancel any time before ONGOING)
 *
 * WHY model this?
 * - Booking Service checks status = OPEN_FOR_BOOKING before allowing booking.
 * - ONGOING/COMPLETED prevents new bookings.
 * - CANCELLED triggers refund flow in Payment Service.
 */
public enum ShowStatus {

    /**
     * Show created but not yet open for booking.
     * E.g., tickets go on sale 1 week before the show date.
     */
    SCHEDULED,

    /**
     * Tickets are available for purchase.
     * Booking Service can create bookings only in this state.
     */
    OPEN_FOR_BOOKING,

    /**
     * Show is currently running in the theatre.
     * No new bookings allowed. Seats cannot be released.
     */
    ONGOING,

    /**
     * Show has ended. All bookings are finalized.
     * Historical record is preserved for analytics/reports.
     */
    COMPLETED,

    /**
     * Show was cancelled by admin.
     * All bookings should be refunded.
     * Booking Service listens for this event (future: via Kafka).
     */
    CANCELLED
}
