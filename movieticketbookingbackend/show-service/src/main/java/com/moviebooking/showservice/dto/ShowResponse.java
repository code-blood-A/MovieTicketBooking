package com.moviebooking.showservice.dto;

import com.moviebooking.showservice.entity.enums.ShowStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * ShowResponse — returned to the client after create/get show operations.
 *
 * Does NOT include the full seat list by default (that's a separate endpoint).
 * Reason: A show can have 150-300 seats. Sending them in every show listing
 * response would be massive. The seat map is fetched on demand via
 * GET /api/shows/{showId}/seats.
 *
 * This is the "summary" view — enough info to display in a show listing card:
 *   "Inception | 6:00 PM | ₹250 | IMAX | 45 seats left | OPEN_FOR_BOOKING"
 */
@Getter
@Builder
public class ShowResponse {

    private Long id;
    private Long movieId;
    private Long screenId;
    private LocalDate showDate;
    private LocalTime showTime;
    private BigDecimal basePrice;
    private Integer durationMinutes;
    private String language;
    private ShowStatus status;

    /** Quick-read seat counts — no need to load all ShowSeat records */
    private Integer totalSeats;
    private Integer availableSeats;

    private LocalDateTime createdAt;
}
