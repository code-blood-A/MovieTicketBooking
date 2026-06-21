package com.moviebooking.showservice.entity;

import com.moviebooking.showservice.entity.enums.ShowStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Show — the core entity of Show Service.
 *
 * A Show represents a single screening of a movie in a specific screen at a
 * specific time.
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ SHOW │
 * │ movieId ─────────── refers to Movie (Movie Service) │
 * │ screenId ─────────── refers to Screen (Theatre Svc) │
 * │ showDate + showTime ── when it plays │
 * │ basePrice ─────────── floor ticket price │
 * │ status ────────────── show lifecycle state │
 * └─────────────────────────────────────────────────────────┘
 *
 * LLD Pattern: No foreign key constraints to other services.
 * - movieId is a plain Long — we trust the caller.
 * - screenId is a plain Long — same reason.
 * - If we needed to validate, we'd call Movie/Theatre via OpenFeign.
 * For now (MVP), we skip cross-service validation for simplicity.
 *
 * Pricing model:
 * basePrice = floor price (regular seats)
 * PREMIUM seats = basePrice × 1.5 (calculated in ShowSeat)
 * RECLINER seats = basePrice × 2.0 (calculated in ShowSeat)
 * Screen type multiplier applied separately (IMAX = +50%, etc.)
 */
@Entity
@Table(name = "shows",
        // A screen cannot have two shows overlapping — enforced at application level
        // (not DB level, since we'd need to check time ranges, not just exact equality)
        indexes = {
                @Index(name = "idx_show_screen_date", columnList = "screen_id, show_date"),
                @Index(name = "idx_show_movie_date", columnList = "movie_id, show_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * References Movie entity in Movie Service's database.
     * We intentionally store this as a plain Long — no @ManyToOne to Movie,
     * because Movie lives in a completely different service and database.
     */
    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    /**
     * References Screen entity in Theatre Service's database.
     * Plain Long — microservice boundary. No JPA relationship.
     * At show creation time, admin provides this ID.
     * Booking Service uses this to render the seat map.
     */
    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    /**
     * The date of the show (e.g., 2024-12-25).
     * Stored separately from time for easy date-based querying.
     * "Show all movies on 2024-12-25" → WHERE show_date = '2024-12-25'
     */
    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    /**
     * The time the show starts (e.g., 18:00).
     * Stored separately for easy time-based filtering.
     */
    @Column(name = "show_time", nullable = false)
    private LocalTime showTime;

    /**
     * Base ticket price for REGULAR seats.
     * Premium/Recliner multipliers are applied in ShowSeat.
     * BigDecimal for financial accuracy — never use float/double for money!
     */
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    /**
     * Duration of the movie in minutes.
     * Stored here (denormalized from Movie Service) to help detect
     * schedule conflicts: a 150-min movie starting at 6PM ends at 8:30PM,
     * so no other show can start before 8:45PM in that screen.
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /**
     * Language of the show (e.g., "Hindi", "English", "Telugu").
     * Same movie can have shows in multiple languages.
     */
    @Column(name = "language", length = 50)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShowStatus status = ShowStatus.SCHEDULED;

    /**
     * Total seats in the screen (denormalized at show creation time).
     * Avoids a cross-service call just to display "available seats".
     */
    @Column(name = "total_seats")
    private Integer totalSeats;

    /**
     * Available seats count — decremented when seats are locked/booked.
     * Used for quick "X seats left" display without counting ShowSeat records.
     * This is a soft count — the source of truth is ShowSeat.status.
     */
    @Column(name = "available_seats")
    private Integer availableSeats;

    /**
     * ShowSeats are generated automatically when a Show is created.
     *
     * CascadeType.ALL: If a Show is deleted, all its ShowSeats are deleted too.
     * orphanRemoval: If a ShowSeat is removed from this list, it's deleted from DB.
     * fetch=LAZY: Don't load all seats when you just want Show metadata.
     *
     * @Builder.Default: Lombok requires this to initialize collections in @Builder.
     *                   Without it, builder().build() gives a null list instead of
     *                   an empty list.
     */
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShowSeat> showSeats = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
