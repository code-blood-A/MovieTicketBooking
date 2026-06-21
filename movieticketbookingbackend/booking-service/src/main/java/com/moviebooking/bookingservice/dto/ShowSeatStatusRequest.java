package com.moviebooking.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ShowSeatStatusRequest — sent to Show Service to update a seat's status.
 *
 * Mirrors Show Service's SeatStatusUpdateRequest DTO.
 *
 * Used in Feign calls:
 *   PUT /api/shows/{showId}/seats/{seatId}/status
 *   Body: { "status": "LOCKED" | "BOOKED" | "AVAILABLE" }
 *
 * WHY duplicate this DTO instead of sharing a common library?
 * ─────────────────────────────────────────────────────────────
 * In true microservice architecture, each service owns its API contract.
 * Sharing a JAR between services creates a coupling dependency:
 *   - Upgrading Show Service forces you to rebuild Booking Service too.
 *   - Version conflicts across services.
 *
 * By duplicating the minimal DTO, Booking Service only depends on the
 * HTTP contract (URL + JSON shape), not on Show Service's internal code.
 * This is the "anti-corruption layer" pattern.
 *
 * Production alternative: publish a separate client library or use
 * OpenAPI-generated clients from Show Service's Swagger spec.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeatStatusRequest {

    /**
     * New status to set on the seat.
     * Possible values: "LOCKED", "BOOKED", "AVAILABLE"
     * Show Service's ShowSeatStatus enum handles parsing.
     */
    private String status;

    /**
     * The user performing this action.
     * Show Service validates this for LOCK operations.
     * Also stored as lockedByUserId for the audit trail.
     */
    private Long userId;
}
