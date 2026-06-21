package com.moviebooking.showservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — centralized error handling for Show Service.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody.
 * Intercepts ALL exceptions thrown from any @RestController and converts
 * them into consistent JSON error responses.
 *
 * WHY centralize exceptions?
 * Without this, Spring returns its default Whitelabel Error Page (HTML),
 * or a stacktrace in JSON — neither is suitable for a REST API client.
 * This ensures ALL errors follow the same shape:
 *   { "status": 404, "message": "...", "timestamp": "..." }
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Standard error response shape — same across all services */
    private record ErrorResponse(int status, String message, LocalDateTime timestamp) {}

    @ExceptionHandler(ShowNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShowNotFound(ShowNotFoundException ex) {
        log.warn("Show not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(ShowSeatNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShowSeatNotFound(ShowSeatNotFoundException ex) {
        log.warn("Show seat not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage(), LocalDateTime.now()));
    }

    /**
     * Handles seat conflict errors (double-booking attempt, seat already booked, etc.).
     * IllegalStateException used as a lightweight domain exception for business rule violations.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage(), LocalDateTime.now()));
    }

    /**
     * Handles @Valid validation failures.
     * Collects ALL field errors into one response (not just the first one).
     * Example: "movieId: Movie ID is required; showDate: Show date is required"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, message, LocalDateTime.now()));
    }

    /** Catch-all for any unexpected exception — TEMPORARILY shows real error for debugging */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error in Show Service", ex);
        // DEBUG MODE: expose real exception so we can identify root cause.
        // TODO: revert to generic message before production.
        String debugMessage = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, debugMessage, LocalDateTime.now()));
    }
}
