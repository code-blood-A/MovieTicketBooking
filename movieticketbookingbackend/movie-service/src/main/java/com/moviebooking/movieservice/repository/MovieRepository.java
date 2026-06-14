package com.moviebooking.movieservice.repository;

import com.moviebooking.movieservice.entity.Movie;
import com.moviebooking.movieservice.entity.enums.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Movie Repository.
 *
 * ═══════════════════════════════════════════════════════════
 * JPQL @Query vs Derived Method Names
 * ═══════════════════════════════════════════════════════════
 * For simple queries: derived method names (findByStatus, findByLanguage)
 * For complex queries with multiple optional parameters: @Query (JPQL or native SQL)
 *
 * JPQL (Java Persistence Query Language):
 * - Queries against ENTITY classes and fields, NOT table/column names
 * - 'FROM Movie m' — Movie is the Java class name
 * - 'm.title' — the Java field name (not the DB column name)
 * - Database-independent: works with MySQL, PostgreSQL, etc.
 *
 * The search query uses COALESCE trick for optional parameters:
 * (:title IS NULL OR ...) — if title param is null, skip that condition.
 * This lets us use ONE query for all combinations of filter params.
 *
 * ═══════════════════════════════════════════════════════════
 * JOIN FETCH — Solving the N+1 Problem
 * ═══════════════════════════════════════════════════════════
 * findByIdWithDetails uses JOIN FETCH to load genres in a single query.
 * Without JOIN FETCH: loading movie + accessing genres = 2 queries (N+1 when in a loop).
 * With JOIN FETCH:    loading movie + genres = 1 query (JOIN in SQL).
 *
 * When to use JOIN FETCH: when you KNOW you'll need the relationship data.
 * The regular findById uses LAZY loading (genres not loaded until accessed).
 * findByIdWithDetails is used for the "movie detail page" where we always need genres+cast.
 */
@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByStatus(MovieStatus status);

    List<Movie> findByLanguageIgnoreCase(String language);

    List<Movie> findByStatusAndLanguageIgnoreCase(MovieStatus status, String language);

    /**
     * Search movies by title (partial match, case-insensitive).
     * LIKE '%:title%' with LOWER() for case-insensitive search.
     */
    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Movie> searchByTitle(@Param("title") String title);

    /**
     * Multi-filter search — all parameters are optional.
     * Passing null for a parameter skips that filter condition.
     *
     * Example calls:
     *   searchMovies("Avengers", null, null)    → search by title only
     *   searchMovies(null, "English", NOW_SHOWING) → by language + status
     *   searchMovies(null, null, null)            → returns all (use findAll instead)
     */
    @Query("SELECT DISTINCT m FROM Movie m WHERE " +
           "(:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:language IS NULL OR LOWER(m.language) = LOWER(:language)) AND " +
           "(:status IS NULL OR m.status = :status)")
    List<Movie> searchMovies(
            @Param("title") String title,
            @Param("language") String language,
            @Param("status") MovieStatus status
    );

    /**
     * Fetch movie WITH genres and casts in a single query.
     * Used for the movie detail endpoint where we need all related data.
     *
     * Note: You can't JOIN FETCH two collections in one query (Hibernate restriction).
     * So we fetch genres here and rely on a separate query for casts (or use EntityGraph).
     * For Phase 1 simplicity, we EAGER-load casts via a second query in the service.
     */
    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres WHERE m.id = :id")
    Optional<Movie> findByIdWithGenres(@Param("id") Long id);
}
