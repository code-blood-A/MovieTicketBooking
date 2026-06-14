package com.moviebooking.theatreservice.controller;

import com.moviebooking.theatreservice.dto.*;
import com.moviebooking.theatreservice.service.TheatreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TheatreController — REST API for Theatre and Screen management.
 *
 * Public (no JWT):  GET /api/theatres/**   — browse theatres/screens
 * Admin only:       POST/PUT/DELETE        — manage theatres/screens
 *
 * The screenId returned from POST /api/theatres/{id}/screens is what
 * the Show Service uses when creating a show (POST /api/shows with screenId).
 */
@RestController
@RequestMapping("/api/theatres")
@RequiredArgsConstructor
@Slf4j
public class TheatreController {

    private final TheatreService theatreService;

    // ── THEATRE ENDPOINTS ─────────────────────────────────────

    @PostMapping
    public ResponseEntity<TheatreResponse> createTheatre(
            @Valid @RequestBody TheatreRequest request,
            @RequestHeader(name = "X-User-Role", required = false) String userRole) {
        if (!"ROLE_ADMIN".equals(userRole)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(theatreService.createTheatre(request));
    }

    @GetMapping
    public ResponseEntity<List<TheatreResponse>> getTheatres(
            @RequestParam(name = "city", required = false) String city) {
        if (city != null && !city.isBlank()) {
            return ResponseEntity.ok(theatreService.getTheatresByCity(city));
        }
        return ResponseEntity.ok(theatreService.getAllTheatres());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TheatreResponse> getTheatreById(@PathVariable Long id) {
        return ResponseEntity.ok(theatreService.getTheatreById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TheatreResponse> updateTheatre(
            @PathVariable Long id,
            @Valid @RequestBody TheatreRequest request,
            @RequestHeader(name = "X-User-Role", required = false) String userRole) {
        if (!"ROLE_ADMIN".equals(userRole)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(theatreService.updateTheatre(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTheatre(
            @PathVariable Long id,
            @RequestHeader(name = "X-User-Role", required = false) String userRole) {
        if (!"ROLE_ADMIN".equals(userRole)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        theatreService.deleteTheatre(id);
        return ResponseEntity.noContent().build();
    }

    // ── SCREEN ENDPOINTS ──────────────────────────────────────

    /**
     * POST /api/theatres/{id}/screens
     * Add a new screen to a theatre. Auto-generates all seats.
     * Returns ScreenDetailResponse with the full seat layout.
     */
    @PostMapping("/{theatreId}/screens")
    public ResponseEntity<ScreenDetailResponse> addScreen(
            @PathVariable Long theatreId,
            @Valid @RequestBody ScreenRequest request,
            @RequestHeader(name = "X-User-Role", required = false) String userRole) {
        if (!"ROLE_ADMIN".equals(userRole)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(theatreService.addScreen(theatreId, request));
    }

    @GetMapping("/{theatreId}/screens")
    public ResponseEntity<List<ScreenSummaryResponse>> getScreensByTheatre(@PathVariable Long theatreId) {
        return ResponseEntity.ok(theatreService.getScreensByTheatre(theatreId));
    }

    /**
     * GET /api/theatres/screens/{screenId}
     * Returns full screen detail with all seats.
     * Used by Show Service (to validate screenId) and Booking Service (to render seat map).
     */
    @GetMapping("/screens/{screenId}")
    public ResponseEntity<ScreenDetailResponse> getScreenById(@PathVariable Long screenId) {
        return ResponseEntity.ok(theatreService.getScreenById(screenId));
    }
}
