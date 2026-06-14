package com.moviebooking.theatreservice.entity;

import com.moviebooking.theatreservice.entity.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Seat entity — an individual seat inside a Screen.
 *
 * ═══════════════════════════════════════════════════════════
 * SEAT NUMBERING CONVENTION
 * ═══════════════════════════════════════════════════════════
 * seatNumber = rowLabel + columnNumber
 * e.g., "A1", "A2", ..., "A15", "B1", ..., "J15"
 *
 *     [SCREEN]
 *  A1 A2 A3 ... A15   ← Row A (REGULAR, front)
 *  B1 B2 B3 ... B15   ← Row B (REGULAR)
 *  ...
 *  I1 I2 I3 ... I15   ← Row I (PREMIUM)
 *  J1 J2 J3 ... J15   ← Row J (RECLINER, back)
 *
 * WHY store rowLabel and columnNumber separately?
 * The Booking Service needs to render a seat map:
 *   "Show me all seats in row C" → WHERE rowLabel = 'C'
 *   "Show me seat C5" → WHERE rowLabel='C' AND columnNumber=5
 * Having them split makes these queries simple and indexed.
 *
 * ═══════════════════════════════════════════════════════════
 * This entity is READ-HEAVY
 * ═══════════════════════════════════════════════════════════
 * Seats are created once (when screen is set up) and almost never updated.
 * But they are READ very frequently by the Booking Service to build seat maps.
 * This makes them a great candidate for Redis caching (Phase 2).
 */
@Entity
@Table(
    name = "seats",
    indexes = {
        @Index(name = "idx_seat_screen", columnList = "screen_id"),
        @Index(name = "idx_seat_screen_type", columnList = "screen_id, seat_type")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The full seat identifier shown to users: "A1", "B12", "J5"
     * Unique within a screen (but not across the whole DB).
     */
    @Column(nullable = false, length = 10)
    private String seatNumber;

    /** Row identifier: "A", "B", "C" ... "Z" (single char for up to 26 rows) */
    @Column(nullable = false, length = 5)
    private String rowLabel;

    /** Column position: 1, 2, 3, ..., seatsPerRow */
    @Column(nullable = false)
    private Integer columnNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatType seatType;    // REGULAR, PREMIUM, or RECLINER

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;
}
