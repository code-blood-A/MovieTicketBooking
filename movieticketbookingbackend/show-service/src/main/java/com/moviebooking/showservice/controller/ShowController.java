package com.moviebooking.showservice.controller;

import com.moviebooking.showservice.dto.*;
import com.moviebooking.showservice.entity.enums.ShowStatus;
import com.moviebooking.showservice.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * ShowController — REST API for Show and ShowSeat management.
 *
 * Endpoint summary:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ PUBLIC (no JWT — readable by anyone browsing shows) │
 * │ GET /api/shows — search/list shows │
 * │ GET /api/shows/{id} — show details │
 * │ GET /api/shows/{id}/seats — seat map for a show │
 * │ GET /api/shows/movie/{movieId} — shows for a movie by date │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ ADMIN (requires ROLE_ADMIN via X-User-Role header) │
 * │ POST /api/shows — create show + seats │
 * │ PATCH /api/shows/{id}/status — change show status │
 * │ DELETE /api/shows/{id} — delete a show │
 * ├─────────────────────────────────────────────────────────────────┤
 * │ AUTHENTICATED (requires logged-in user via X-User-Id header) │
 * │ PUT /api/shows/{showId}/seats/{seatId}/status — lock/book seat │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * The Gateway's JwtAuthenticationFilter enforces:
 * - GET /api/shows/** → always public
 * - POST/PUT/PATCH/DELETE → require valid JWT
 * This controller additionally checks ROLE_ADMIN for admin operations.
 */
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@Slf4j
public class ShowController {

    private final ShowService showService;

    // ── SHOW CRUD ─────────────────────────────────────────────────

    /**
     * POST /api/shows
     * Create a new show and auto-generate seat records.
     * Admin only.
     */
    @PostMapping
    public ResponseEntity<ShowResponse> createShow(
            @Valid @RequestBody ShowRequest request,
            @RequestHeader(name = "X-User-Role", required = false) String userRole) {
        System.out.println("aakash-controller");
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(showService.createShow(request));
    }

    /**
     * GET /api/shows/{id}
     * Get show details by ID. Public.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(showService.getShowById(id));
    }

    /**
     * GET
     * /api/shows?movieId=X&screenId=Y&date=2024-12-25&language=Hindi&status=OPEN_FOR_BOOKING
     * Multi-filter search. All query params are optional.
     * Used by the frontend to list available shows.
     *
     * All @RequestParam use explicit name= to avoid -parameters reflection issue
     * (Spring Boot 3.2+ requirement when not using spring-boot-starter-parent).
     */
    @GetMapping
    public ResponseEntity<List<ShowResponse>> searchShows(
            @RequestParam(name = "movieId", required = false) Long movieId,
            @RequestParam(name = "screenId", required = false) Long screenId,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "language", required = false) String language,
            @RequestParam(name = "status", required = false) ShowStatus status) {

        return ResponseEntity.ok(
                showService.searchShows(movieId, screenId, date, language, status));
    }

    /**
     * GET /api/shows/movie/{movieId}?date=2024-12-25
     * Get all shows for a specific movie on a specific date.
     * Used by: "Select showtime" screen after user picks a movie.
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponse>> getShowsByMovie(
            @PathVariable("movieId") Long movieId,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (date != null) {
            return ResponseEntity.ok(showService.getShowsByMovieAndDate(movieId, date));
        }
        // No date filter — return all upcoming shows for this movie
        return ResponseEntity.ok(
                showService.searchShows(movieId, null, null, null, null));
    }

    /**
     * PATCH /api/shows/{id}/status
     * Admin updates show lifecycle status.
     * Example: PATCH /api/shows/5/status?status=OPEN_FOR_BOOKING
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ShowResponse> updateStatus(
            @PathVariable("id") Long id,
            @RequestParam(name = "status") ShowStatus status,
            @RequestHeader(name = "X-User-Role", required = false) String userRole) {

        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(showService.updateShowStatus(id, status));
    }

    /**
     * DELETE /api/shows/{id}
     * Admin deletes a show. Cascades to all ShowSeat records.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShow(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Role", required = false) String userRole) {

        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        showService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }

    // ── SEAT ENDPOINTS ────────────────────────────────────────────

    /**
     * GET /api/shows/{showId}/seats
     * Returns the full seat map for a show.
     * Public — users need to see available seats before logging in.
     * Used to render the interactive seat selection grid in the frontend.
     */
    @GetMapping("/{showId}/seats")
    public ResponseEntity<List<ShowSeatResponse>> getSeats(@PathVariable("showId") Long showId) {
        return ResponseEntity.ok(showService.getSeatsByShow(showId));
    }

    /**
     * PUT /api/shows/{showId}/seats/{seatId}/status
     * Update the booking status of a seat.
     *
     * Called by Booking Service (service-to-service) to:
     * - LOCK a seat when user enters checkout
     * - BOOK a seat after payment confirmed
     * - Release LOCK back to AVAILABLE if payment fails
     *
     * X-User-Id injected by Gateway from the JWT token.
     * Only authenticated users can lock/book a seat.
     */
    @PutMapping("/{showId}/seats/{seatId}/status")
    public ResponseEntity<ShowSeatResponse> updateSeatStatus(
            @PathVariable("showId") Long showId,
            @PathVariable("seatId") Long seatId,
            @Valid @RequestBody SeatStatusUpdateRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        // Populate userId from the Gateway-injected header if not in request body
        if (request.getUserId() == null && userId != null) {
            request.setUserId(userId);
        }

        return ResponseEntity.ok(showService.updateSeatStatus(showId, seatId, request));
    }
}
