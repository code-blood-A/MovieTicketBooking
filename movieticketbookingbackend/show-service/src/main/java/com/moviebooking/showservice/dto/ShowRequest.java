package com.moviebooking.showservice.dto;

import com.moviebooking.showservice.entity.enums.ShowStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ShowRequest — DTO for creating a new show (admin only).
 *
 * WHY do we need seatData here?
 * ──────────────────────────────
 * When a show is created, we auto-generate ShowSeat records.
 * But to know HOW MANY seats and WHAT TYPE each seat is,
 * we need the seat layout from Theatre Service.
 *
 * Options:
 *   A) Call Theatre Service via OpenFeign at show creation time (correct for prod)
 *   B) Admin provides totalSeats + seat breakdown manually (simpler for MVP)
 *
 * We go with option B for now (MVP simplicity).
 * The admin provides: screenId (which has the seats), totalRows, seatsPerRow,
 * premiumRows, reclinerRows — same data they used when creating the screen.
 *
 * MVP shortcut noted: In a production system, Show Service would call
 * GET /api/theatres/screens/{screenId} via OpenFeign to fetch the seat layout
 * automatically without requiring the admin to re-enter it.
 */
@Getter
@Setter
public class ShowRequest {

    /**
     * Which movie is being screened.
     * Must be a valid movieId from Movie Service (no cross-service validation in MVP).
     */
    @NotNull(message = "Movie ID is required")
    private Long movieId;

    /**
     * Which screen this show is scheduled in.
     * Must be a valid screenId from Theatre Service.
     * The screen determines the seat layout and total capacity.
     */
    @NotNull(message = "Screen ID is required")
    private Long screenId;

    @NotNull(message = "Show date is required")
    private LocalDate showDate;

    @NotNull(message = "Show time is required")
    private LocalTime showTime;

    /**
     * Base ticket price for REGULAR seats.
     * PREMIUM and RECLINER prices are auto-calculated as multipliers:
     *   PREMIUM  = basePrice × 1.5
     *   RECLINER = basePrice × 2.0
     */
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal basePrice;

    /** Movie duration in minutes — used to detect scheduling conflicts */
    private Integer durationMinutes;

    /** Language of this specific show (same movie can run in Hindi + English) */
    private String language;

    /**
     * Total number of rows in the screen.
     * Used with seatsPerRow to auto-generate ShowSeat records.
     * Must match what was set when the screen was created in Theatre Service.
     */
    @NotNull(message = "Total rows required to generate seats")
    @Min(value = 1) @Max(value = 26)
    private Integer totalRows;

    /**
     * Seats per row in the screen.
     * totalRows × seatsPerRow = total ShowSeat records created.
     */
    @NotNull(message = "Seats per row required to generate seats")
    @Min(value = 1) @Max(value = 50)
    private Integer seatsPerRow;

    /**
     * Row labels that are PREMIUM type (e.g., ["H", "I"]).
     * These rows get basePrice × 1.5 pricing.
     */
    private java.util.List<String> premiumRows;

    /**
     * Row labels that are RECLINER type (e.g., ["J"]).
     * These rows get basePrice × 2.0 pricing.
     */
    private java.util.List<String> reclinerRows;
}
