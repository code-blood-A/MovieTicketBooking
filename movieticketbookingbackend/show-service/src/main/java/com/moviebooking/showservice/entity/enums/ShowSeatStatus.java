package com.moviebooking.showservice.entity.enums;

/**
 * ShowSeatStatus — tracks the booking state of a single seat for a specific show.
 *
 * This is one of the most critical pieces of the booking system.
 * Without proper state management here, double-booking occurs.
 *
 * State machine:
 *
 *   AVAILABLE ──(user selects)──▶ LOCKED ──(payment confirmed)──▶ BOOKED
 *                                    │
 *                                    └──(payment timeout/failed)──▶ AVAILABLE
 *
 * WHY "LOCKED" state?
 * ─────────────────
 * Problem: User A and User B both see seat E5 as AVAILABLE.
 *          Both try to book simultaneously.
 *          Without LOCKED, both succeed → double booking!
 *
 * Solution: When User A selects the seat, we immediately set it to LOCKED
 *           with a 10-minute TTL. During this window:
 *           - Show seat appears as unavailable to User B.
 *           - User A completes payment → BOOKED.
 *           - If User A abandons → after 10 min, scheduler resets to AVAILABLE.
 *
 * Future enhancement: Redis-based distributed lock for the LOCKED timeout
 *                     (current: DB-based with a lockedAt timestamp).
 */
public enum ShowSeatStatus {

    /**
     * Seat is free to book. Shown as green in the seat map UI.
     */
    AVAILABLE,

    /**
     * Seat is temporarily held by a user during the checkout flow.
     * Will automatically revert to AVAILABLE after timeout if not paid.
     * Shown as grey/pending in the seat map UI.
     */
    LOCKED,

    /**
     * Seat has been paid for. Booking is confirmed.
     * Shown as red in the seat map UI.
     */
    BOOKED
}
