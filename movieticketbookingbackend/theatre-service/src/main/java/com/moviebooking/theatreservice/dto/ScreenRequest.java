package com.moviebooking.theatreservice.dto;

import com.moviebooking.theatreservice.entity.enums.ScreenType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * ScreenRequest — used to create a new screen inside a theatre.
 *
 * KEY CONCEPT — Seat Auto-Generation:
 * The admin provides: totalRows=10, seatsPerRow=15
 * The service auto-generates 150 seats: A1..A15, B1..B15, ..., J1..J15
 *
 * premiumRows: list of row labels that should be PREMIUM (e.g., ["H", "I"])
 * reclinerRows: list of row labels that should be RECLINER (e.g., ["J"])
 * Rows not in either list → REGULAR by default.
 */
@Getter @Setter
public class ScreenRequest {

    @NotBlank(message = "Screen name is required")
    private String name;

    private ScreenType screenType;   // Default: TWO_D if not provided

    @NotNull(message = "Total rows required")
    @Min(value = 1, message = "Minimum 1 row")
    @Max(value = 26, message = "Maximum 26 rows (A-Z)")
    private Integer totalRows;

    @NotNull(message = "Seats per row required")
    @Min(value = 1, message = "Minimum 1 seat per row")
    @Max(value = 50, message = "Maximum 50 seats per row")
    private Integer seatsPerRow;

    /** Row labels to be marked PREMIUM. e.g., ["H", "I"] */
    private List<String> premiumRows;

    /** Row labels to be marked RECLINER. e.g., ["J"] */
    private List<String> reclinerRows;
}
