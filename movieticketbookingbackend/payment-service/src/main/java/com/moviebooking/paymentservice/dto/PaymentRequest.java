package com.moviebooking.paymentservice.dto;

import com.moviebooking.paymentservice.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * PaymentRequest — sent by the client to initiate a payment.
 *
 * Flow:
 *   1. User creates booking → receives PENDING booking with bookingId + totalAmount.
 *   2. User selects payment method (UPI, card, etc.).
 *   3. Client sends PaymentRequest to POST /api/payments/initiate.
 *   4. Payment Service creates a PENDING payment record.
 *   5. Client calls POST /api/payments/{id}/process to simulate gateway.
 *
 * In a real integration:
 *   Step 4: Payment Service creates an order in Razorpay/Stripe.
 *   Step 5: Client is redirected to gateway's hosted payment page.
 *   Step 6: Gateway calls Payment Service's webhook after success/failure.
 */
@Getter
@Setter
public class PaymentRequest {

    /**
     * The booking being paid for.
     * Payment Service will verify this booking is in PENDING state
     * before creating a payment record.
     */
    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    /**
     * Amount to charge — should match the booking's totalAmount.
     * Client sends this so Payment Service can do a sanity check.
     * (Prevents "pay ₹1 for a ₹500 booking" attacks.)
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * How the user wants to pay.
     * In production: different methods trigger different gateway flows.
     * For MVP: just recorded — no actual gateway routing.
     */
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
