package com.moviebooking.movieservice.entity;

import com.moviebooking.movieservice.entity.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Movie entity — the central entity of the Movie Service.
 *
 * ═══════════════════════════════════════════════════════════
 * JPA RELATIONSHIPS IN THIS ENTITY
 * ═══════════════════════════════════════════════════════════
 *
 * 1. @ManyToMany with Genre (OWNING SIDE)
 *    Movie is the owner → defines the @JoinTable.
 *    The join table 'movie_genres' has two FK columns.
 *    Why Movie is the owner: conceptually, a movie "has genres", not the other way.
 *
 * 2. @OneToMany with Cast (parent side)
 *    One Movie has many Cast records.
 *    cascade = ALL: saving/deleting a Movie cascades to its Cast records.
 *    orphanRemoval = true: if a Cast is removed from the list, it's deleted from DB.
 *    This makes Movie the "aggregate root" for its cast.
 *
 * ═══════════════════════════════════════════════════════════
 * FetchType.LAZY vs EAGER — IMPORTANT CONCEPT
 * ═══════════════════════════════════════════════════════════
 * EAGER: Load the relationship immediately when the parent is loaded.
 *   SELECT * FROM movies WHERE id = ?;
 *   SELECT * FROM movie_genres WHERE movie_id = ?;   ← immediate
 *   SELECT * FROM genres WHERE id IN (...);           ← immediate
 *
 * LAZY: Load the relationship ONLY when it's first accessed in code.
 *   SELECT * FROM movies WHERE id = ?;
 *   ... later, when you call movie.getGenres() ...
 *   SELECT * FROM movie_genres WHERE movie_id = ?;   ← triggered on first access
 *
 * WHY LAZY for @ManyToMany genres?
 * Listing 100 movies for a homepage: you DON'T need genres for each.
 * With EAGER, every single movie query would also query genres — 100 extra queries!
 * With LAZY: genres are loaded ONLY when you explicitly access them.
 *
 * The N+1 problem: If you LAZY-load in a loop (for each movie, access genres),
 * you get 1 query for movies + N queries for genres = N+1 total.
 * Solution: Use @EntityGraph or JOIN FETCH in the query when you need genres.
 * We solve this in the service layer with a specific query for movie details.
 */
@Entity
@Table(name = "movies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")   // TEXT type in MySQL for long descriptions
    private String description;

    @Column(nullable = false, length = 50)
    private String language;              // "Hindi", "English", "Tamil", "Telugu"

    @Column(nullable = false)
    private Integer durationMinutes;      // e.g., 169 for Avengers: Endgame

    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MovieStatus status = MovieStatus.COMING_SOON;

    /** Average user rating 0.0 - 10.0 */
    @Builder.Default
    private Double rating = 0.0;

    /** URL to the movie poster image (S3, CDN, etc.) */
    @Column(length = 500)
    private String posterUrl;

    /**
     * Many-to-many with Genre.
     * This is the OWNING SIDE — defines the join table.
     *
     * @JoinTable defines:
     *   name = "movie_genres"       → the join table name in MySQL
     *   joinColumns = movie_id      → FK column pointing to THIS entity (Movie)
     *   inverseJoinColumns = genre_id → FK column pointing to the OTHER entity (Genre)
     *
     * Result in MySQL:
     *   CREATE TABLE movie_genres (
     *     movie_id BIGINT NOT NULL,
     *     genre_id BIGINT NOT NULL,
     *     PRIMARY KEY (movie_id, genre_id),
     *     FOREIGN KEY (movie_id) REFERENCES movies(id),
     *     FOREIGN KEY (genre_id) REFERENCES genres(id)
     *   );
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "movie_genres",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @Builder.Default
    private List<Genre> genres = new ArrayList<>();

    /**
     * One-to-many with Cast.
     * mappedBy = "movie" refers to the Cast.movie field (the FK is in the cast table).
     * cascade = ALL: persist/delete cascades to cast records.
     * orphanRemoval = true: removing a cast from this list deletes the DB row.
     */
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Cast> casts = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
