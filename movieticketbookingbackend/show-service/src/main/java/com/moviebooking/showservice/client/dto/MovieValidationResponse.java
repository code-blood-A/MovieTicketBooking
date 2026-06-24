package com.moviebooking.showservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MovieValidationResponse — mirrors Movie Service's MovieResponse.
 *
 * WHY a separate DTO instead of importing Movie Service's class?
 * ────────────────────────────────────────────────────────────────
 * Anti-corruption layer pattern: Show Service must NOT depend on
 * Movie Service's internal classes. If Movie Service changes its DTO,
 * Show Service should not break — it only cares about the fields it needs.
 *
 * We only map the fields Show Service actually uses:
 *   - id            → confirm the movie exists
 *   - durationMinutes → auto-populate on Show entity (no admin re-entry)
 *   - title         → logging / future rich response
 *
 * Fields like genres, casts, posterUrl etc. are irrelevant to Show Service
 * → not mapped here (Jackson ignores unknown fields by default).
 *
 * Jackson deserialization requirement:
 *   @NoArgsConstructor is required so Jackson can construct the object
 *   and then set fields via setters. Without it → deserialization error.
 */
@Getter
@Setter
@NoArgsConstructor
public class MovieValidationResponse {
    private Long id;
    private String title;
    private Integer durationMinutes;
    private String status;   // UPCOMING / NOW_SHOWING / ENDED
}
