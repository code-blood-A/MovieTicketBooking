package com.moviebooking.showservice.repository;

import com.moviebooking.showservice.entity.Show;
import com.moviebooking.showservice.entity.enums.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * ShowRepository — database queries for Show entities.
 *
 * Uses Spring Data JPA derived queries where possible (method name → SQL).
 * Uses @Query for complex multi-field searches with optional parameters.
 */
@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    /**
     * Find all shows for a specific movie on a specific date.
     * Used by: "Show all showtimes for Inception on 2024-12-25"
     * Sorted by showTime ascending (earliest show first).
     */
    List<Show> findByMovieIdAndShowDateOrderByShowTimeAsc(Long movieId, LocalDate showDate);

    /**
     * Find all shows in a specific screen on a specific date.
     * Used by: Show Service to detect scheduling conflicts.
     * (Can't put a 6PM show if 4PM show ends at 6:30PM)
     */
    List<Show> findByScreenIdAndShowDateOrderByShowTimeAsc(Long screenId, LocalDate showDate);

    /**
     * Find all shows for a movie (across all dates).
     * Used by: Admin panel to see upcoming screenings of a movie.
     */
    List<Show> findByMovieIdAndStatusNot(Long movieId, ShowStatus status);

    /**
     * Multi-filter show search with optional parameters.
     *
     * WHY JPQL instead of derived method name?
     * A method like findByMovieIdAndShowDateAndLanguage would REQUIRE all three
     * parameters to be non-null. With JPQL + COALESCE/OR pattern, each param
     * is optional — if null, that filter is skipped.
     *
     * COALESCE trick: (:movieId IS NULL OR s.movieId = :movieId)
     *   → if movieId param is null → condition is TRUE (skip filter)
     *   → if movieId param is provided → must match
     *
     * Used by: GET /api/shows?movieId=5&date=2024-12-25&language=Hindi
     */
    @Query("""
            SELECT s FROM Show s
            WHERE (:movieId IS NULL OR s.movieId = :movieId)
              AND (:screenId IS NULL OR s.screenId = :screenId)
              AND (:showDate IS NULL OR s.showDate = :showDate)
              AND (:language IS NULL OR LOWER(s.language) = LOWER(:language))
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.showDate ASC, s.showTime ASC
            """)
    List<Show> searchShows(
            @Param("movieId") Long movieId,
            @Param("screenId") Long screenId,
            @Param("showDate") LocalDate showDate,
            @Param("language") String language,
            @Param("status") ShowStatus status
    );
}
