package com.moviebooking.movieservice.entity;

import com.moviebooking.movieservice.entity.enums.CastRole;
import jakarta.persistence.*;
import lombok.*;

/**
 * Cast entity — represents a person's involvement in a movie.
 *
 * ═══════════════════════════════════════════════════════════
 * @ManyToOne — the "many" side of Movie ↔ Cast
 * ═══════════════════════════════════════════════════════════
 * Many Cast records → One Movie.
 * The FK column 'movie_id' lives in the 'movie_cast' table (this entity's table).
 * This is always how it works: the FK is on the @ManyToOne side.
 *
 * FetchType.LAZY: When we load a Cast record, don't automatically load
 * the entire Movie. We rarely need the full Movie when working with cast data.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY "movie_cast" table name (not "cast")?
 * ═══════════════════════════════════════════════════════════
 * "cast" is a RESERVED KEYWORD in MySQL!
 * Using it as a table name would cause SQL errors.
 * Always check reserved keywords when naming entities.
 */
@Entity
@Table(name = "movie_cast")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;          // Person's name e.g., "Aamir Khan"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CastRole role;        // ACTOR, DIRECTOR, PRODUCER

    @Column(length = 150)
    private String characterName; // Only relevant for ACTOR: e.g., "Rancho"

    /**
     * The FK back to Movie. JPA stores this as 'movie_id' column in movie_cast table.
     * nullable = false: every cast record MUST belong to a movie.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;
}
