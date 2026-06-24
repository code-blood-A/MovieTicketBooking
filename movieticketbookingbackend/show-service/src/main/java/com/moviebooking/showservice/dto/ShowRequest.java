package com.moviebooking.showservice.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * ShowRequest — DTO for creating a new show (admin only).
 *
 * PRODUCTION UPGRADE (from MVP):
 * ─────────────────────────────────────────────────────────
 * Fields removed vs. MVP version:
 *   - durationMinutes → fetched from Movie Service via Feign (authoritative source)
 *   - totalRows       → fetched from Theatre Service via Feign (authoritative source)
 *   - seatsPerRow     → fetched from Theatre Service via Feign (authoritative source)
 *
 * WHY remove them?
 *   In MVP, admin manually typed these values — error-prone.
 *   E.g., screen has 10 rows × 15 seats = 150 physical seats.
 *   Admin types 12 rows → ShowService creates 180 ShowSeats → inconsistency!
 *
 *   With Feign validation:
 *   1. Admin provides movieId + screenId.
 *   2. Show Service calls Movie Service → gets real durationMinutes.
 *   3. Show Service calls Theatre Service → gets real totalRows + seatsPerRow.
 *   4. ShowSeat count ALWAYS matches physical screen capacity.
 *
 * Fields kept in request (business decisions, not physical facts):
 *   - premiumRows / reclinerRows → admin decides which rows have premium pricing.
 *     Theatre Service knows seat positions, not pricing tiers.
 */
@Getter
@Setter
public class ShowRequest {

    /**
     * Which movie is being screened.
     * Validated via Feign → Movie Service. Also fetches durationMinutes.
     */
    @NotNull(message = "Movie ID is required")
    private Long movieId;

    /**
     * Which screen this show is scheduled in.
     * Validated via Feign → Theatre Service. Also fetches totalRows, seatsPerRow.
     */
    @NotNull(message = "Screen ID is required")
    private Long screenId;

    @NotNull(message = "Show date is required")
    private LocalDate showDate;

    @NotNull(message = "Show time is required")
    private LocalTime showTime;

    /**
     * Base ticket price for REGULAR seats.
     * PREMIUM  = basePrice × 1.5
     * RECLINER = basePrice × 2.0
     */
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal basePrice;

    /** Language of this specific show (same movie can run in Hindi + English) */
    @NotNull(message = "Language is required")
    private String language;

    /**
     * Row labels that are PREMIUM type (e.g., ["H", "I"]).
     * Admin decides this — Theatre Service has no concept of pricing tiers.
     * Optional — if null, no rows are PREMIUM.
     */
    private List<String> premiumRows;

    /**
     * Row labels that are RECLINER type (e.g., ["J"]).
     * Optional — if null, no rows are RECLINER.
     */
    private List<String> reclinerRows;
}
