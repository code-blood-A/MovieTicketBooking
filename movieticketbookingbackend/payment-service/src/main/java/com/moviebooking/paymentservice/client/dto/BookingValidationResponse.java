package com.moviebooking.paymentservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * BookingValidationResponse — mirror DTO for Booking Service's BookingResponse.
 *
 * Anti-corruption layer: Payment Service does NOT import Booking Service's classes.
 * We only map what Payment Service needs to validate a booking before payment:
 *
 *   id          → confirm the booking exists
 *   userId      → verify the payer is the booking owner (prevent payment by others)
 *   status      → must be PENDING before we accept payment
 *   totalAmount → compare against requested payment amount (sanity check)
 *
 * Jackson ignores unknown fields (items[], showId, etc.) by default.
 * @NoArgsConstructor required for Jackson deserialization.
 */
@Getter
@Setter
@NoArgsConstructor
public class BookingValidationResponse {
    private Long id;
    private Long userId;
    private String status;        // "PENDING" / "CONFIRMED" / "CANCELLED" / "FAILED"
    private BigDecimal totalAmount;
}
