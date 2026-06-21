package com.moviebooking.paymentservice.entity;

import com.moviebooking.paymentservice.entity.enums.PaymentMethod;
import com.moviebooking.paymentservice.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment — the core entity of Payment Service.
 *
 * ┌───────────────────────────────────────────────────────────┐
 * │  PAYMENT                                                  │
 * │  bookingId  ─── which booking this pays for               │
 * │  userId     ─── who is paying                             │
 * │  amount     ─── total amount charged                      │
 * │  method     ─── UPI / CREDIT_CARD / etc.                  │
 * │  status     ─── PENDING → SUCCESS / FAILED                │
 * │  txnId      ─── simulated transaction reference ID        │
 * └───────────────────────────────────────────────────────────┘
 *
 * Cross-service references:
 * ─────────────────────────
 * bookingId → booking_db (Booking Service's database)
 * userId    → user_db (User Service's database)
 * Both are plain Long — no JPA FK across service boundaries.
 *
 * Idempotency:
 * ────────────
 * @UniqueConstraint on bookingId: one booking → one payment record.
 * If a client accidentally calls initiate twice for the same booking,
 * the DB unique constraint prevents duplicate payment records.
 * The second call gets a DataIntegrityViolationException → 409 Conflict.
 *
 * Transaction ID:
 * ───────────────
 * In production, this would be the payment gateway's reference ID
 * (e.g., Razorpay order_id, Stripe charge_id).
 * For MVP, we generate a simulated ID (e.g., "TXN-1718888400000-42").
 * The user would show this on their receipt for support queries.
 */
@Entity
@Table(
    name = "payments",
    uniqueConstraints = {
        // One payment per booking — prevents duplicate charges
        @UniqueConstraint(name = "uk_payment_booking", columnNames = "booking_id")
    },
    indexes = {
        // Fast lookup: "All payments by user Y" → payment history screen
        @Index(name = "idx_payment_user", columnList = "user_id"),
        // Fast lookup by status → admin reports
        @Index(name = "idx_payment_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The booking this payment is for.
     * Unique — one booking = one payment record.
     */
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    /**
     * The user paying.
     * Injected from X-User-Id header (Gateway reads JWT, injects header).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Amount to charge. Must match the booking's totalAmount.
     * BigDecimal — exact decimal arithmetic for financial data.
     * Validated in PaymentRequest: must match booking's total.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Payment method chosen by the user.
     * Stored as STRING: "UPI", "CREDIT_CARD" etc.
     * Readable in DB without needing to know enum ordinals.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /**
     * Current payment lifecycle state.
     * Starts as PENDING → becomes SUCCESS or FAILED after processing.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Simulated transaction reference ID.
     * Format: "TXN-{timestamp}-{bookingId}"
     * In production: gateway's transaction ID (Razorpay/Stripe reference).
     * Used by support team to trace payment issues.
     */
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    /**
     * Optional failure reason when status = FAILED.
     * Examples: "Insufficient funds", "Card declined", "UPI timeout"
     * In production: gateway's error code/message.
     * For MVP: set to "Payment declined by user" when success=false in processPayment.
     */
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
