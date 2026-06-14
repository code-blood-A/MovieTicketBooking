package com.moviebooking.movieservice.controller;

import com.moviebooking.movieservice.dto.*;
import com.moviebooking.movieservice.entity.enums.MovieStatus;
import com.moviebooking.movieservice.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MovieController — REST endpoints for Movie and Genre management.
 *
 * ═══════════════════════════════════════════════════════════
 * ADMIN CHECK via X-User-Role header
 * ═══════════════════════════════════════════════════════════
 * GET endpoints → public (whitelisted in Gateway)
 * POST/PUT/DELETE → require ROLE_ADMIN
 *
 * The Gateway injects X-User-Role header ONLY when a valid JWT is present.
 * For public routes (no JWT), the header will be absent.
 * required = false → Spring won't throw 400 if the header is missing.
 * We then explicitly check for ROLE_ADMIN in the method body.
 *
 * In Phase 5 (production): use Spring Security @PreAuthorize("hasRole('ADMIN')")
 * with proper JWT filter in each service. For Phase 1, header check is sufficient.
 */
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {

    private final MovieService movieService;

    // ── GENRE ENDPOINTS ──────────────────────────────────────

    @PostMapping("/genres")
    public ResponseEntity<GenreResponse> createGenre(
            @Valid @RequestBody GenreRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.createGenre(request));
    }

    @GetMapping("/genres")
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        return ResponseEntity.ok(movieService.getAllGenres());
    }

    // ── MOVIE ENDPOINTS ──────────────────────────────────────

    /**
     * GET /api/movies — List all movies (public).
     * Optional query params: ?status=NOW_SHOWING or ?title=Avengers&language=English
     *
     * WHY combine list + search in one endpoint?
     * REST best practice: use query parameters for filtering on collection endpoints.
     * GET /api/movies?status=NOW_SHOWING is cleaner than GET /api/movies/now-showing.
     */
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getMovies(
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "language", required = false) String language,
            @RequestParam(name = "status", required = false) MovieStatus status) {

        // If any search param is provided, use search query
        if (title != null || language != null || status != null) {
            return ResponseEntity.ok(movieService.searchMovies(title, language, status));
        }
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(
            @Valid @RequestBody MovieRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.createMovie(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @Valid @RequestBody MovieRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
