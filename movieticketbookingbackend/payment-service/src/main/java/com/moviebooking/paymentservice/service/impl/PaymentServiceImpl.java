package com.moviebooking.paymentservice.service.impl;

import com.moviebooking.paymentservice.client.BookingServiceClient;
import com.moviebooking.paymentservice.client.dto.BookingValidationResponse;
import com.moviebooking.paymentservice.dto.*;
import com.moviebooking.paymentservice.entity.Payment;
import com.moviebooking.paymentservice.entity.enums.PaymentStatus;
import com.moviebooking.paymentservice.exception.PaymentNotFoundException;
import com.moviebooking.paymentservice.repository.PaymentRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * PaymentServiceImpl — the full payment orchestration logic.
 *
 * ═══════════════════════════════════════════════════════════
 * PAYMENT FLOW OVERVIEW
 * ═══════════════════════════════════════════════════════════
 *
 * Step 1 — initiatePayment():
 *   Client sends { bookingId, amount, paymentMethod }.
 *   We create a Payment record with status=PENDING.
 *   No money moves yet. No call to Booking Service yet.
 *   Returns paymentId that the client will use in Step 2.
 *
 * Step 2 — processPayment():
 *   Client sends { success: true/false } (simulates gateway callback).
 *
 *   If success=true:
 *     → Update Payment status to SUCCESS.
 *     → Generate transactionId (simulated gateway reference).
 *     → Feign: POST /api/bookings/{bookingId}/confirm
 *       → Booking Service: seats LOCKED → BOOKED, Booking → CONFIRMED.
 *
 *   If success=false:
 *     → Update Payment status to FAILED.
 *     → Store failureReason ("Insufficient funds" etc.).
 *     → Feign: POST /api/bookings/{bookingId}/cancel
 *       → Booking Service: seats → AVAILABLE, Booking → CANCELLED.
 *
 * ═══════════════════════════════════════════════════════════
 * IDEMPOTENCY
 * ═══════════════════════════════════════════════════════════
 * The Payment table has a UNIQUE constraint on booking_id.
 * If initiatePayment() is called twice for the same booking:
 *   → 2nd call: DataIntegrityViolationException (DB unique constraint)
 *   → GlobalExceptionHandler → 409 Conflict
 *   → Client sees: "Payment already exists for this booking"
 *
 * ═══════════════════════════════════════════════════════════
 * TRANSACTION ID GENERATION (MVP)
 * ═══════════════════════════════════════════════════════════
 * Format: "TXN-{epochMillis}-{bookingId}"
 * Example: "TXN-1718888400000-42"
 * In production: UUID or Razorpay/Stripe's generated ID.
 * Epoch millis ensure uniqueness without a UUID generator dependency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl {

    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingServiceClient;

    // ─────────────────────────────────────────────────────────────────
    // INITIATE PAYMENT
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, Long userId) {
        log.info("Initiating payment: bookingId={}, amount={}, method={}, userId={}",
                request.getBookingId(), request.getAmount(), request.getPaymentMethod(), userId);

        // ── Step 1: Validate booking exists in Booking Service ────────────
        // Payment Service must NOT trust blind bookingId values from client.
        // We call Booking Service via Feign to verify:
        //   a) Booking exists (not a made-up ID)
        //   b) Booking is PENDING (not already CONFIRMED or CANCELLED)
        //   c) Booking belongs to this user (prevent paying for someone else's booking)
        //   d) Amount matches the booking's totalAmount (prevent underpaying)
        BookingValidationResponse booking;
        try {
            booking = bookingServiceClient.getBooking(request.getBookingId(), userId, null);
        } catch (FeignException.NotFound ex) {
            // Booking Service returned 404 — bookingId simply doesn't exist
            throw new IllegalArgumentException(
                    "Booking ID " + request.getBookingId() + " does not exist. " +
                    "Please create a booking first before initiating payment.");
        } catch (FeignException.Forbidden ex) {
            // Booking Service returned 403 — booking exists but belongs to another user
            throw new IllegalArgumentException(
                    "Booking ID " + request.getBookingId() + " does not belong to you. " +
                    "You can only pay for your own bookings.");
        } catch (FeignException ex) {
            // Booking Service is down or returned unexpected error
            log.error("Could not reach Booking Service to validate bookingId={}: {}",
                    request.getBookingId(), ex.getMessage());
            throw new RuntimeException(
                    "Booking Service is currently unavailable. Please try again shortly.");
        }

        // ── Step 2: Business rule — booking must be PENDING ──────────────
        // Prevent paying for a booking that is already CONFIRMED (double charge)
        // or CANCELLED (seats released, can't pay for cancelled booking).
        if (!"PENDING".equals(booking.getStatus())) {
            throw new IllegalStateException(
                    "Cannot initiate payment for booking ID " + request.getBookingId() +
                    ". Booking status is '" + booking.getStatus() + "' but must be PENDING.");
        }

        // ── Step 3: Amount sanity check ─────────────────────────────────
        // Prevent "pay ₹1 for a ₹500 booking" client manipulation.
        // The real amount must come from the booking, not the client's request.
        if (booking.getTotalAmount() != null &&
                request.getAmount().compareTo(booking.getTotalAmount()) != 0) {
            throw new IllegalArgumentException(
                    "Payment amount " + request.getAmount() +
                    " does not match booking total " + booking.getTotalAmount() +
                    ". Please use the exact booking amount.");
        }

        // ── Step 4: Idempotency — don't create duplicate payment ─────────
        if (paymentRepository.findByBookingId(request.getBookingId()).isPresent()) {
            throw new IllegalStateException(
                    "A payment already exists for booking ID: " + request.getBookingId() +
                    ". Cannot initiate duplicate payment.");
        }

        // ── Step 5: Create PENDING payment record ───────────────────────
        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(userId)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment initiated: paymentId={}, bookingId={}, status=PENDING",
                saved.getId(), saved.getBookingId());

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────
    // PROCESS PAYMENT (simulate gateway callback)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Simulates receiving a payment gateway callback.
     *
     * success=true  → payment approved → confirm booking
     * success=false → payment declined → cancel booking (release seats)
     *
     * In production:
     *   This method body would be inside a webhook controller that:
     *   1. Receives signed webhook payload from Razorpay/Stripe.
     *   2. Verifies the HMAC signature (prevent fake webhooks).
     *   3. Extracts the payment ID and status from the payload.
     *   4. Calls this same confirm/cancel logic.
     */
    @Transactional
    public PaymentResponse processPayment(Long paymentId, ProcessPaymentRequest request, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment " + paymentId + " is already " + payment.getStatus() +
                    ". Cannot process again.");
        }

        if (request.getSuccess()) {
            // ── PAYMENT SUCCESS ────────────────────────────────────────
            // Generate a simulated transaction ID
            String txnId = "TXN-" + Instant.now().toEpochMilli() + "-" + payment.getBookingId();
            payment.setTransactionId(txnId);
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            log.info("Payment {} SUCCESS. TxnId={}. Confirming booking {}...",
                    paymentId, txnId, payment.getBookingId());

            // Feign: tell Booking Service to confirm the booking
            try {
                bookingServiceClient.confirmBooking(payment.getBookingId(), userId);
                log.info("Booking {} confirmed after payment success", payment.getBookingId());
            } catch (FeignException ex) {
                // Payment succeeded but booking confirmation failed.
                // This is a critical inconsistency — log for manual resolution.
                // In production: publish to a dead-letter queue / alert ops team.
                log.error("CRITICAL: Payment {} succeeded but booking {} confirmation failed: {}",
                        paymentId, payment.getBookingId(), ex.getMessage());
                // Don't throw — payment is already SUCCESS, we just couldn't confirm the booking
                // Support team needs to manually resolve using the transactionId
            }

        } else {
            // ── PAYMENT FAILED ─────────────────────────────────────────
            String reason = request.getFailureReason() != null
                    ? request.getFailureReason()
                    : "Payment declined";

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);

            log.info("Payment {} FAILED. Reason: {}. Cancelling booking {}...",
                    paymentId, reason, payment.getBookingId());

            // Feign: tell Booking Service to cancel (releases seat locks)
            try {
                bookingServiceClient.cancelBooking(payment.getBookingId(), userId);
                log.info("Booking {} cancelled after payment failure. Seats released.", payment.getBookingId());
            } catch (FeignException ex) {
                // Payment failed AND booking cancellation failed.
                // Seats might be stuck LOCKED — need cleanup job or manual fix.
                log.error("Failed to cancel booking {} after payment failure: {}",
                        payment.getBookingId(), ex.getMessage());
            }
        }

        return toResponse(payment);
    }

    // ─────────────────────────────────────────────────────────────────
    // READ OPERATIONS
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        return toResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId)));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        return toResponse(paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment found for booking ID: " + bookingId)));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // MAPPING HELPER
    // ─────────────────────────────────────────────────────────────────

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
