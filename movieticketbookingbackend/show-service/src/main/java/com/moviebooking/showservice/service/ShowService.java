package com.moviebooking.showservice.service;

import com.moviebooking.showservice.dto.*;
import com.moviebooking.showservice.entity.enums.ShowStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * ShowService — business logic contract for Show Service.
 *
 * Interface + Impl pattern:
 * - Decouples the API contract from the implementation.
 * - Enables easy mocking in unit tests.
 * - Allows swapping implementations (e.g., caching layer, OpenFeign version).
 */
public interface ShowService {

    /** Create a new show and auto-generate all ShowSeat records */
    ShowResponse createShow(ShowRequest request);

    /** Get show details by ID */
    ShowResponse getShowById(Long id);

    /**
     * Multi-filter show search.
     * All parameters are optional — pass null to skip that filter.
     * Supports: movieId, screenId, date, language, status combinations.
     */
    List<ShowResponse> searchShows(Long movieId, Long screenId, LocalDate showDate,
                                   String language, ShowStatus status);

    /** Get all shows for a specific movie on a specific date */
    List<ShowResponse> getShowsByMovieAndDate(Long movieId, LocalDate date);

    /** Update show status (SCHEDULED → OPEN_FOR_BOOKING → CANCELLED etc.) */
    ShowResponse updateShowStatus(Long id, ShowStatus newStatus);

    /** Delete a show (admin only; cascades to all ShowSeats) */
    void deleteShow(Long id);

    // ─── Seat Operations ──────────────────────────────────────────

    /** Get full seat map for a show (for UI seat selection) */
    List<ShowSeatResponse> getSeatsByShow(Long showId);

    /**
     * Update the status of a specific seat in a show.
     * Called by Booking Service to LOCK → BOOKED or release LOCK → AVAILABLE.
     */
    ShowSeatResponse updateSeatStatus(Long showId, Long seatId, SeatStatusUpdateRequest request);
}
