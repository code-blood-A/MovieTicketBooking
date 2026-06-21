package com.moviebooking.bookingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * BookingItem — one row per seat within a Booking.
 *
 * WHY does BookingItem exist?
 * ─────────────────────────────
 * A user can book MULTIPLE seats in one transaction:
 *   "Book seats A1, A2, A3 for show 42 → total ₹450"
 *
 * BookingItem stores the per-seat details AT THE TIME OF BOOKING.
 * This is important because prices can change after booking:
 *   - Admin increases the base price for a future show.
 *   - RECLINER multiplier changes.
 *
 * If we only stored showSeatId and re-fetched the price when needed,
 * the receipt would show a wrong (updated) price.
 *
 * By capturing seatLabel, seatType, price into BookingItem,
 * the booking receipt is a SNAPSHOT in time — immutable and correct
 * regardless of future price changes. This is the standard pattern
 * in e-commerce systems (order line items, invoice items etc.).
 *
 * Data Model:
 * ───────────
 *   Booking (1) ─────── (N) BookingItem
 *   Each BookingItem holds:
 *     showSeatId  → the ShowSeat's PK in show_db (our reference, not FK)
 *     seatLabel   → "A1", "J15" — denormalized for receipt display
 *     seatType    → REGULAR/PREMIUM/RECLINER — denormalized
 *     price       → captured at booking time — immutable
 */
@Entity
@Table(
    name = "booking_items",
    indexes = {
        // Fast lookup: "All items for booking X" → used in getBookingById
        @Index(name = "idx_bookingitem_booking", columnList = "booking_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent Booking this item belongs to.
     *
     * ManyToOne because many items belong to one booking.
     * FetchType.LAZY: don't load the full Booking when we just need seat info.
     *
     * @JoinColumn: creates a booking_id FK column in booking_items table.
     * nullable = false: every BookingItem must belong to a Booking.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * The ShowSeat's primary key in show_db.
     * Plain Long — cross-service reference, no JPA FK.
     * Used by Booking Service to call:
     *   PUT /api/shows/{showId}/seats/{showSeatId}/status → BOOKED
     */
    @Column(name = "show_seat_id", nullable = false)
    private Long showSeatId;

    /**
     * Human-readable seat label (e.g., "E5", "J12").
     * Copied from Show Service at booking time.
     * Shown on the ticket/receipt — avoids calling Show Service at read time.
     */
    @Column(name = "seat_label", length = 10, nullable = false)
    private String seatLabel;

    /**
     * Seat type (REGULAR, PREMIUM, RECLINER).
     * Copied at booking time — snapshot pattern.
     * Used on the receipt to explain why seat J12 costs ₹300 vs A1 at ₹150.
     */
    @Column(name = "seat_type", length = 20, nullable = false)
    private String seatType;

    /**
     * Exact price paid for THIS seat at booking time.
     * BigDecimal — exact decimal arithmetic for financial data.
     *
     * Snapshot pattern: even if Show Service later changes the base price
     * for this show, this value stays as what the user actually paid.
     * Critical for accurate receipts and refund calculations.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
