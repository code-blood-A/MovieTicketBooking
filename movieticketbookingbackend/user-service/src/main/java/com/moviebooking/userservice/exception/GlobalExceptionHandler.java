package com.moviebooking.userservice.exception;

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
 * GlobalExceptionHandler — Centralized error handling for all controllers.
 *
 * ═══════════════════════════════════════════════════════════
 * LLD PATTERN: Template Method (sort of) + Single Responsibility
 * ═══════════════════════════════════════════════════════════
 * Without this class: every controller method would need try-catch blocks.
 * With @RestControllerAdvice: exceptions bubble up from any controller,
 * and this class intercepts them centrally.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * It's a global component that applies to ALL @RestController classes.
 *
 * ═══════════════════════════════════════════════════════════
 * CONSISTENT ERROR RESPONSE FORMAT
 * ═══════════════════════════════════════════════════════════
 * Every error returns the same JSON structure:
 * {
 *   "status": 409,
 *   "message": "An account with email 'x@y.com' already exists",
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * Consistency is critical for API consumers (mobile apps, frontend) —
 * they handle ONE error format, not a different structure per exception.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY NOT use @ResponseStatus on exceptions alone?
 * ═══════════════════════════════════════════════════════════
 * @ResponseStatus sets the HTTP status but returns an empty or default body.
 * @ExceptionHandler lets you return a rich, structured JSON body.
 * We use both: @ResponseStatus on exceptions (as documentation/fallback),
 * and @ExceptionHandler here for the actual response body.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles duplicate email registration.
     * Returns: 409 Conflict + error message
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(
            UserAlreadyExistsException ex) {
        log.warn("UserAlreadyExistsException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles user not found + invalid login credentials.
     * Returns: 404 Not Found + error message
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UserNotFoundException ex) {
        log.warn("UserNotFoundException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles Bean Validation failures (@Valid on request DTOs).
     *
     * MethodArgumentNotValidException contains ALL validation errors.
     * We collect them all into a single comma-separated message so the
     * client knows exactly which fields failed and why.
     *
     * Example response body:
     * {
     *   "status": 400,
     *   "message": "email: Please provide a valid email address, password: must be at least 8 characters",
     *   "timestamp": "..."
     * }
     *
     * Returns: 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errorMessage);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    /**
     * Catch-all handler for any unexpected exception.
     * Returns: 500 Internal Server Error
     *
     * IMPORTANT: We log the full stacktrace here (log.error with ex param)
     * but return a GENERIC message to the client. Never expose internal
     * details (stack traces, DB errors) to clients — security risk.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    /**
     * Builds a consistent error response body.
     * Using Map<String, Object> instead of a dedicated ErrorResponse class
     * keeps it simple. In a larger project, use a dedicated ErrorResponse record.
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
