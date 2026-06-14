package com.moviebooking.movieservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Genre entity — represents a movie genre (Action, Drama, Comedy, etc.)
 *
 * ═══════════════════════════════════════════════════════════
 * JPA RELATIONSHIP: @ManyToMany (bidirectional)
 * ═══════════════════════════════════════════════════════════
 * One Genre can belong to many Movies ("Action" → Avengers, RRR, etc.)
 * One Movie can have many Genres (Avengers → Action, Sci-Fi)
 * This is a classic many-to-many relationship.
 *
 * In the database, this creates a JOIN TABLE: movie_genres
 *   movie_genres (movie_id FK, genre_id FK)
 *
 * In JPA:
 * - The OWNING side (Movie) declares @JoinTable
 * - The INVERSE side (Genre) declares mappedBy="genres"
 * - mappedBy tells JPA: "the Movie.genres field manages this relationship"
 *
 * WHY mappedBy matters:
 * Without mappedBy, JPA would create TWO join tables (one per side).
 * With mappedBy on Genre, JPA knows there's only ONE join table (defined on Movie).
 *
 * ═══════════════════════════════════════════════════════════
 * @JsonIgnore on movies
 * ═══════════════════════════════════════════════════════════
 * If you serialize a Genre entity directly (which we avoid by using DTOs),
 * Jackson would serialize Genre → movies → Genre → movies → ... (infinite loop).
 * @JsonIgnore breaks this cycle by not serializing the movies list from Genre.
 * We rely on DTOs anyway, so this is just a safety net.
 */
@Entity
@Table(name = "genres")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;  // "Action", "Drama", "Comedy", "Thriller", "Romance", "Horror"

    /**
     * Inverse side of the many-to-many relationship.
     * mappedBy = "genres" means: "the 'genres' field on the Movie class owns this relationship".
     *
     * FetchType.LAZY: Don't load the movies list unless explicitly accessed.
     * Loading all movies for a genre every time we fetch a genre would be extremely expensive.
     */
    @ManyToMany(mappedBy = "genres", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Movie> movies = new ArrayList<>();
}
