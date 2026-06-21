package com.moviebooking.paymentservice.controller;

import com.moviebooking.paymentservice.dto.*;
import com.moviebooking.paymentservice.service.impl.PaymentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PaymentController — REST API for Payment operations.
 *
 * All endpoints require JWT (enforced by API Gateway).
 * userId is read from X-User-Id header — never from request body.
 *
 * Endpoint summary:
 * ┌───────────────────────────────────────────────────────────────────┐
 * │  POST /api/payments/initiate              — start a payment       │
 * │  POST /api/payments/{id}/process          — simulate gateway      │
 * │  GET  /api/payments/{id}                  — get payment by ID     │
 * │  GET  /api/payments/booking/{bookingId}   — get by booking ID     │
 * │  GET  /api/payments/my                    — my payment history    │
 * └───────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentServiceImpl paymentService;

    /**
     * POST /api/payments/initiate
     * Creates a PENDING payment record for a booking.
     *
     * Client must send: { bookingId, amount, paymentMethod }
     * Returns: Payment with status=PENDING and a paymentId.
     *
     * Client uses the paymentId to call /process next.
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiatePayment(request, userId));
    }

    /**
     * POST /api/payments/{id}/process
     * Simulates a payment gateway callback.
     *
     * Body: { "success": true }  → confirms booking, marks payment SUCCESS
     * Body: { "success": false } → cancels booking, marks payment FAILED
     *
     * In production, this would be a webhook endpoint called by
     * Razorpay/Stripe after the user completes payment on their hosted page.
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProcessPaymentRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(paymentService.processPayment(id, request, userId));
    }

    /**
     * GET /api/payments/{id}
     * Get a specific payment record by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    /**
     * GET /api/payments/booking/{bookingId}
     * Find the payment for a specific booking.
     * NOTE: defined before /{id} to avoid "booking" being parsed as a Long.
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(
            @PathVariable("bookingId") Long bookingId,
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    /**
     * GET /api/payments/my
     * Get all payments made by the authenticated user.
     * NOTE: defined before /{id} to avoid "my" being parsed as a Long.
     */
    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(paymentService.getMyPayments(userId));
    }
}
