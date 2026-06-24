package com.moviebooking.showservice.client;

import com.moviebooking.showservice.client.dto.ScreenValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * TheatreServiceClient — Feign Client for validating screens.
 *
 * @FeignClient(name = "theatre-service"):
 *   Resolves via Eureka to the actual IP:port of theatre-service.
 *
 * Used in ShowServiceImpl.createShow():
 *   Validates that the screenId exists AND fetches totalRows, seatsPerRow,
 *   totalSeats — so Show Service auto-generates the seat layout without
 *   requiring the admin to re-enter those values manually.
 *
 * This eliminates a big class of bugs:
 *   BEFORE: Admin creates screen with 10 rows × 15 seats = 150 seats.
 *           Then creates show and accidentally types 12 rows → 180 ShowSeats
 *           → Show has MORE ShowSeats than physical seats exist!
 *
 *   AFTER:  Show Service fetches totalRows=10, seatsPerRow=15 from Theatre Service.
 *           ShowSeat count ALWAYS matches physical reality.
 */
@FeignClient(name = "theatre-service")
public interface TheatreServiceClient {

    /**
     * Fetches a screen by ID to validate it exists and get its seat layout.
     * Maps to: GET /api/theatres/screens/{id} in Theatre Service's TheatreController.
     *
     * @param id The screenId from ShowRequest
     * @return ScreenValidationResponse with totalRows, seatsPerRow, totalSeats
     * @throws feign.FeignException.NotFound if screen doesn't exist (404)
     */
    @GetMapping("/api/theatres/screens/{id}")
    ScreenValidationResponse getScreen(@PathVariable("id") Long id);
}
