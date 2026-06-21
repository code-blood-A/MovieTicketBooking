package com.moviebooking.showservice.entity;

import com.moviebooking.showservice.entity.enums.ShowSeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ShowSeat — represents a single seat's booking status for ONE specific show.
 *
 * WHY does ShowSeat exist? Why not just use Theatre's Seat directly?
 * ─────────────────────────────────────────────────────────────────
 * Theatre's Seat entity is a physical seat (Row E, Seat 5, PREMIUM type).
 * It exists permanently and doesn't change.
 *
 * But for EACH show, the same physical seat needs its own booking state:
 *   - For the 3PM show: Seat E5 is BOOKED (by User A)
 *   - For the 6PM show: Seat E5 is AVAILABLE (nobody has booked it yet)
 *   - For the 9PM show: Seat E5 is LOCKED (User B is in checkout)
 *
 * ShowSeat bridges the gap: each Show gets its own copy of all seat states.
 *
 * Data Model:
 *   Show (1) ─────────── (N) ShowSeat
 *   Each ShowSeat holds:
 *     - seatId (from Theatre Service — our reference, not FK)
 *     - seatLabel (e.g., "E5" — denormalized for display without cross-service call)
 *     - seatType (REGULAR/PREMIUM/RECLINER — denormalized)
 *     - price (calculated: basePrice × seatType multiplier)
 *     - status (AVAILABLE/LOCKED/BOOKED)
 *     - lockedBy (userId who locked it — for validation during payment)
 *     - lockedAt (when it was locked — for TTL expiry calculation)
 *
 * Booking flow:
 *   1. User selects seat → POST /api/shows/{showId}/seats/{seatId}/lock
 *      → ShowSeat.status = LOCKED, lockedBy = userId, lockedAt = now
 *   2. User pays → Booking Service calls PUT /api/shows/{showId}/seats/{seatId}/book
 *      → ShowSeat.status = BOOKED, booking created in booking_db
 *   3. User abandons → scheduled job (future) resets LOCKED seats after 10 min
 */
@Entity
@Table(
    name = "show_seats",
    indexes = {
        // Fast lookup: "What's the status of seat X in show Y?"
        @Index(name = "idx_showseat_show_seat", columnList = "show_id, seat_id"),
        // Fast lookup: "All available seats for show Y"
        @Index(name = "idx_showseat_show_status", columnList = "show_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The Show this seat belongs to.
     * ManyToOne because many ShowSeats belong to one Show.
     * fetch=LAZY: Don't load the full Show when we just need seat info.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    /**
     * Reference to the physical seat ID in Theatre Service.
     * Plain Long — no FK. We use this to identify which seat was booked
     * when communicating with Booking Service.
     *
     * nullable = true (MVP): We generate seats without calling Theatre Service
     * to fetch real seat IDs. In production, OpenFeign would populate this
     * from GET /api/theatres/screens/{screenId}/seats.
     */
    @Column(name = "seat_id")
    private Long seatId;

    /**
     * Human-readable seat label (e.g., "E5", "J12").
     * Denormalized from Theatre Service at show creation time.
     * Avoids cross-service call just to display "Your seat: E5" on a ticket.
     */
    @Column(name = "seat_label", length = 10, nullable = false)
    private String seatLabel;

    /**
     * Seat type stored as STRING for readability.
     * Denormalized — copied from Theatre's Seat.seatType when show is created.
     * Used to calculate the price multiplier.
     */
    @Column(name = "seat_type", length = 20, nullable = false)
    private String seatType; // REGULAR, PREMIUM, RECLINER

    /**
     * Final ticket price for THIS seat in THIS show.
     * Calculated at show creation: basePrice × seatTypeMultiplier
     *   REGULAR: basePrice × 1.0
     *   PREMIUM:  basePrice × 1.5
     *   RECLINER: basePrice × 2.0
     * BigDecimal — never float/double for money!
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShowSeatStatus status = ShowSeatStatus.AVAILABLE;

    /**
     * ID of the user who currently holds the LOCK on this seat.
     * Null when AVAILABLE or BOOKED.
     * Injected from the X-User-Id header by the Gateway.
     */
    @Column(name = "locked_by_user_id")
    private Long lockedByUserId;

    /**
     * When this seat was locked (for TTL expiry).
     * Background scheduler (future enhancement) will query:
     *   WHERE status = 'LOCKED' AND locked_at < NOW() - 10 minutes
     *   → reset those to AVAILABLE
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;
}
