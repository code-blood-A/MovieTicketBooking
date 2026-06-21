package com.moviebooking.showservice.dto;

import com.moviebooking.showservice.entity.enums.ShowSeatStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * SeatStatusUpdateRequest — used by Booking Service to lock or book a seat.
 *
 * PUT /api/shows/{showId}/seats/{seatId}/status
 *
 * Callers:
 *   - Booking Service locks a seat  → { "status": "LOCKED",  "userId": 42 }
 *   - Booking Service books a seat  → { "status": "BOOKED",  "userId": 42 }
 *   - Booking Service releases lock → { "status": "AVAILABLE","userId": 42 }
 *
 * Security note:
 *   This endpoint is NOT public. Only internal service-to-service calls
 *   (from Booking Service) should use this. In production, add an internal
 *   API key or mTLS between services. For MVP, it's behind JWT via the gateway.
 */
@Getter
@Setter
public class SeatStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private ShowSeatStatus status;

    /** The user performing the action — validated against the locked seat's lockedByUserId */
    private Long userId;
}
