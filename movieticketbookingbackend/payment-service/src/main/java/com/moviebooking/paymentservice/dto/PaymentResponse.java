package com.moviebooking.paymentservice.dto;

import com.moviebooking.paymentservice.entity.enums.PaymentMethod;
import com.moviebooking.paymentservice.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentResponse — full payment summary returned to the client.
 *
 * Returned by all Payment Service endpoints.
 */
@Getter
@Builder
public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private Long userId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
