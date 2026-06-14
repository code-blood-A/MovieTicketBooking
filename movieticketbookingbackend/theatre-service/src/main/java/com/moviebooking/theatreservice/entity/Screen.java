package com.moviebooking.theatreservice.entity;

import com.moviebooking.theatreservice.entity.enums.ScreenType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen entity — an auditorium inside a Theatre.
 * Also called "Audi" or "Hall" in real cinema systems.
 *
 * ═══════════════════════════════════════════════════════════
 * KEY DESIGN: Seats are auto-generated when a Screen is created
 * ═══════════════════════════════════════════════════════════
 * Admin provides: totalRows=10, seatsPerRow=15
 * Service auto-generates: 150 Seat entities (A1..A15, B1..B15, ... J1..J15)
 *
 * WHY auto-generate? Manual seat creation would be tedious (150 API calls).
 * This is a Factory-like behavior: Screen is the factory, Seats are products.
 *
 * The Show Service references screenId when creating a show.
 * The Booking Service references seatId when booking a specific seat.
 *
 * ═══════════════════════════════════════════════════════════
 * totalSeats is DERIVED but STORED
 * ═══════════════════════════════════════════════════════════
 * We could compute totalSeats = totalRows × seatsPerRow dynamically.
 * But storing it as a column means the Show Service can query
 * "available seats" without joining to the seats table:
 *   total - booked = available
 * This is a deliberate denormalization for query performance.
 */
@Entity
@Table(name = "screens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;         // "Screen 1", "Audi 2", "IMAX Hall"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScreenType screenType = ScreenType.TWO_D;

    @Column(nullable = false)
    private Integer totalRows;      // How many rows of seats (e.g., 10 → A to J)

    @Column(nullable = false)
    private Integer seatsPerRow;    // How many seats per row (e.g., 15)

    @Column(nullable = false)
    private Integer totalSeats;     // = totalRows × seatsPerRow (stored for fast queries)

    /**
     * Many-to-one with Theatre — the FK 'theatre_id' lives in this table.
     * LAZY: when loading a screen, don't auto-load the full theatre.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theatre_id", nullable = false)
    private Theatre theatre;

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
