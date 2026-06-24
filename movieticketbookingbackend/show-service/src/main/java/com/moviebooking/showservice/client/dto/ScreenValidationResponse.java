package com.moviebooking.showservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * ScreenValidationResponse — mirrors Theatre Service's ScreenDetailResponse.
 *
 * Fields Show Service uses from the screen:
 *   - id          → confirm the screen exists
 *   - totalSeats  → stored on Show for quick availability display
 *   - totalRows   → used to auto-generate ShowSeat records (rows A to X)
 *   - seatsPerRow → seats per row for ShowSeat generation
 *   - theatreId   → logged / stored for context
 *   - theatreName → for logging and future rich response
 *
 * Fields NOT needed:
 *   - seats[]     → actual physical seats (Feign client returns them, we ignore)
 *   - screenType  → IMAX/2D/4DX etc. — not relevant to Show Service
 */
@Getter
@Setter
@NoArgsConstructor
public class ScreenValidationResponse {
    private Long id;
    private String name;
    private Integer totalRows;
    private Integer seatsPerRow;
    private Integer totalSeats;
    private Long theatreId;
    private String theatreName;
    // seats[] from Theatre Service is intentionally ignored —
    // Show Service generates its own ShowSeat records
}
