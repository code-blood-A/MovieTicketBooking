package com.moviebooking.bookingservice.entity;

import com.moviebooking.bookingservice.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Booking — the central entity of Booking Service.
 *
 * ┌───────────────────────────────────────────────────────┐
 * │ BOOKING │
 * │ userId ─────── who made this booking │
 * │ showId ─────── which show was booked │
 * │ status ─────── PENDING → CONFIRMED / CANCELLED │
 * │ total ─────── sum of all seat prices │
 * │ items[] ─────── one BookingItem per seat │
 * └───────────────────────────────────────────────────────┘
 *
 * LLD Pattern — Cross-service references as plain Long:
 * ───────────────────────────────────────────────────────
 * userId → comes from X-User-Id header (injected by Gateway from JWT)
 * showId → provided in the BookingRequest by the client
 *
 * We do NOT store a foreign key to User or Show entities because
 * they live in different databases (user_db, show_db).
 * In a production system with distributed tracing, you'd correlate
 * these using the booking ID as a correlation key across services.
 *
 * Denormalization note:
 * ─────────────────────
 * showId is stored here so Booking Service can quickly answer:
 * "All bookings for show X" (admin view, for seat availability counts)
 * without calling Show Service.
 */
@Entity
@Table(name = "bookings", indexes = {
        // Fast query: "All bookings by user Y" → for user's booking history
        @Index(name = "idx_booking_user", columnList = "user_id"),
        // Fast query: "All bookings for show X" → for admin show reports
        @Index(name = "idx_booking_show", columnList = "show_id"),
        // Fast query: "All PENDING bookings" → for cleanup job
        @Index(name = "idx_booking_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who made this booking.
     * Populated from X-User-Id header (injected by Gateway from JWT).
     * NOT a foreign key — userId lives in user_db.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The show being booked.
     * Provided by the client in BookingRequest.
     * NOT a foreign key — showId lives in show_db.
     */
    @Column(name = "show_id", nullable = false)
    private Long showId;

    /**
     * Total amount for this booking.
     * Calculated as sum of all BookingItem prices.
     *
     * Denormalization: We store this here so we can display
     * "Your booking total: ₹450" without summing BookingItems each time.
     *
     * BigDecimal — NEVER use double/float for money.
     * float/double use binary floating point → 100.1 + 200.2 ≠ 300.3 exactly.
     * BigDecimal uses decimal arithmetic → exact representation.
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Booking lifecycle state.
     * Stored as STRING ("PENDING", "CONFIRMED") — human-readable in DB,
     * not affected by enum reordering (unlike ORDINAL storage).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    /**
     * The individual seats in this booking.
     *
     * CascadeType.ALL: saving Booking auto-saves all BookingItems.
     * orphanRemoval: if an item is removed from the list, it's deleted from DB.
     * FetchType.LAZY: don't load items when we just need booking metadata.
     *
     * @Builder.Default: Lombok requires this for collection initialization
     *                   inside @Builder. Without it, builder.build() gives null
     *                   list.
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BookingItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
