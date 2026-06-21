package com.moviebooking.bookingservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * ShowSeatResponse — mirrors Show Service's ShowSeatResponse DTO.
 *
 * Received from Show Service when Booking Service calls:
 *   PUT /api/shows/{showId}/seats/{seatId}/status
 *
 * We use only a subset of the fields Show Service returns:
 *   - id         → to store as showSeatId in BookingItem
 *   - seatLabel  → to show on ticket ("E5")
 *   - seatType   → to show on ticket ("PREMIUM")
 *   - price      → to calculate booking total + store in BookingItem
 *   - status     → to verify the lock actually succeeded
 *
 * @NoArgsConstructor: Feign's Jackson deserializer needs a no-arg
 * constructor to create the object and then set fields via setters.
 * Without @NoArgsConstructor, Jackson throws:
 * "No suitable constructor found for type ShowSeatResponse"
 */
@Getter
@Setter
@NoArgsConstructor
public class ShowSeatResponse {

    private Long id;
    private Long seatId;
    private String seatLabel;
    private String seatType;
    private BigDecimal price;
    private String status;    // "AVAILABLE", "LOCKED", "BOOKED"
}
