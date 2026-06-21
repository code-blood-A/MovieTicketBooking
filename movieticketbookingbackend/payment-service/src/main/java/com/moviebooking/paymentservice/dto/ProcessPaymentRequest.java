package com.moviebooking.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * ProcessPaymentRequest — simulates a payment gateway callback.
 *
 * In production:
 *   This would be replaced by a webhook from Razorpay/Stripe.
 *   The gateway calls Payment Service's webhook endpoint with a
 *   signed payload confirming success/failure.
 *   Payment Service verifies the signature, then updates payment status.
 *
 * For MVP:
 *   The client (you, via Postman) calls POST /api/payments/{id}/process
 *   with {"success": true} to simulate "payment approved" or
 *   {"success": false} to simulate "payment declined".
 *
 *   This lets us test the full confirm/cancel flow without a real gateway.
 */
@Getter
@Setter
public class ProcessPaymentRequest {

    /**
     * true  → payment approved → booking CONFIRMED, seats BOOKED
     * false → payment declined → booking CANCELLED, seats released to AVAILABLE
     */
    @NotNull(message = "success field is required (true or false)")
    private Boolean success;

    /**
     * Optional reason for failure.
     * Stored in Payment.failureReason for support reference.
     * Examples: "Insufficient funds", "Card expired", "UPI limit exceeded"
     */
    private String failureReason;
}
