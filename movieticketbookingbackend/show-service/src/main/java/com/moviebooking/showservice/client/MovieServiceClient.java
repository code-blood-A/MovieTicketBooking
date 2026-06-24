package com.moviebooking.showservice.client;

import com.moviebooking.showservice.client.dto.MovieValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * MovieServiceClient — Feign Client for validating movies.
 *
 * @FeignClient(name = "movie-service"):
 *   Resolves via Eureka to the actual IP:port of movie-service.
 *   No hardcoded URLs — scaling/redeployment of Movie Service is transparent.
 *
 * Used in ShowServiceImpl.createShow():
 *   Validates that the movieId exists AND fetches durationMinutes
 *   so Show Service doesn't have to trust admin-provided values.
 *
 * Error behavior:
 *   If Movie Service returns 404 → Feign throws FeignException.NotFound
 *   ShowServiceImpl catches this → throws ShowCreationException("Movie not found")
 *   → Client gets a clear 400 Bad Request with "Movie ID 999 does not exist"
 */
@FeignClient(name = "movie-service")
public interface MovieServiceClient {

    /**
     * Fetches a movie by ID to validate it exists and get its duration.
     * Maps to: GET /api/movies/{id} in Movie Service's MovieController.
     *
     * @param id The movieId from ShowRequest
     * @return MovieValidationResponse with id, title, durationMinutes, status
     * @throws feign.FeignException.NotFound if movie doesn't exist (404)
     */
    @GetMapping("/api/movies/{id}")
    MovieValidationResponse getMovie(@PathVariable("id") Long id);
}
