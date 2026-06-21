package com.moviebooking.paymentservice.repository;

import com.moviebooking.paymentservice.entity.Payment;
import com.moviebooking.paymentservice.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PaymentRepository — Spring Data JPA repository for Payment entities.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by bookingId.
     * Used by: GET /api/payments/booking/{bookingId}
     * Also used internally to check if payment already exists for a booking
     * (idempotency check before creating a new payment record).
     */
    Optional<Payment> findByBookingId(Long bookingId);

    /**
     * Get all payments made by a user.
     * Used by: admin or user payment history view.
     */
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Get payments by status.
     * Used by: admin dashboard — "How many payments failed today?"
     */
    List<Payment> findByStatus(PaymentStatus status);
}
