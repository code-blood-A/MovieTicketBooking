package com.moviebooking.bookingservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — centralized error handling for Booking Service.
 *
 * Maps every exception type to a consistent JSON response:
 *   { "status": 4xx/5xx, "message": "...", "timestamp": "..." }
 *
 * WHY centralize?
 * Without this, Spring returns HTML error pages or raw stack traces.
 * REST API clients expect consistent JSON — this ensures that.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private record ErrorResponse(int status, String message, LocalDateTime timestamp) {}

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(BookingNotFoundException ex) {
        log.warn("Booking not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage(), LocalDateTime.now()));
    }

    /**
     * SeatLockException → 409 Conflict.
     * Tells the client a seat is already taken (so they can re-select).
     */
    @ExceptionHandler(SeatLockException.class)
    public ResponseEntity<ErrorResponse> handleSeatLock(SeatLockException ex) {
        log.warn("Seat lock failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage(), LocalDateTime.now()));
    }

    /**
     * IllegalStateException → 409 Conflict.
     * Used for business rule violations (confirming a non-PENDING booking etc.)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage(), LocalDateTime.now()));
    }

    /**
     * IllegalArgumentException → 400 Bad Request.
     * Used when the client sends an invalid showId or seatId that doesn't exist
     * in Show Service (Show Service returns 404, we catch FeignException.NotFound
     * and re-throw as IllegalArgumentException with a clear message).
     *
     * WHY 400 and not 404?
     * The Booking resource itself wasn't found (that would be 404).
     * The REQUEST itself is invalid — the client sent a bad showId/seatId.
     * 400 = "Your request has an error". 404 = "The resource you asked for doesn't exist".
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, ex.getMessage(), LocalDateTime.now()));
    }

    /**
     * @Valid validation failures → 400 Bad Request.
     * Collects ALL field errors into one response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, message, LocalDateTime.now()));
    }

    /** Catch-all for unexpected exceptions */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error in Booking Service", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "An unexpected error occurred", LocalDateTime.now()));
    }
}
