package com.moviebooking.paymentservice.entity.enums;

/**
 * PaymentStatus — lifecycle states of a Payment record.
 *
 * State machine:
 * ──────────────
 *
 *   [Payment initiated]
 *          ↓
 *       PENDING ──── (gateway response: declined) ──→ FAILED
 *          │
 *          │ (gateway response: approved)
 *          ↓
 *       SUCCESS ──── (refund requested) ──→ REFUNDED
 *
 * WHY track payment status separately from booking status?
 * ─────────────────────────────────────────────────────────
 * They represent different things:
 *   BookingStatus = seat reservation state (PENDING/CONFIRMED/CANCELLED)
 *   PaymentStatus = money movement state (PENDING/SUCCESS/FAILED/REFUNDED)
 *
 * A booking can be CONFIRMED while a payment shows REFUNDED
 * (edge case: admin confirms booking but refund was processed separately).
 *
 * Keeping them decoupled allows each service to evolve independently.
 * Payment Service doesn't need to know about seat types or show schedules.
 * Booking Service doesn't need to know about payment methods or gateway codes.
 */
public enum PaymentStatus {

    /**
     * Payment record created. Waiting for gateway processing.
     * Money has NOT moved yet.
     */
    PENDING,

    /**
     * Payment gateway approved the transaction.
     * Money has been charged. Booking will be CONFIRMED.
     */
    SUCCESS,

    /**
     * Payment gateway declined (insufficient funds, card expired, etc.)
     * or user abandoned the payment flow.
     * Booking will be CANCELLED, seat locks released.
     */
    FAILED,

    /**
     * Payment was successful but the user later requested a refund.
     * Booking status may still be CONFIRMED or CANCELLED depending on
     * when the refund was processed.
     * (Future feature — not implemented in MVP)
     */
    REFUNDED
}
