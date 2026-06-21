package com.moviebooking.paymentservice.entity.enums;

/**
 * PaymentMethod — how the user chose to pay.
 *
 * Stored as STRING in the database for readability.
 *
 * In production (Razorpay/Stripe integration):
 *   - CREDIT_CARD / DEBIT_CARD → card number, CVV, expiry via PCI-DSS gateway
 *   - UPI → UPI ID (e.g., user@paytm) → VPA (Virtual Payment Address)
 *   - NET_BANKING → bank selection → redirect to bank's login page
 *   - WALLET → Paytm, PhonePe, Google Pay wallet balance
 *
 * For MVP, we only record WHICH method was selected.
 * No actual card processing — the gateway call is simulated.
 */
public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    UPI,
    NET_BANKING,
    WALLET
}
