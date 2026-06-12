package com.moviebooking.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a registration attempt uses an email that already exists in the DB.
 * Maps to HTTP 409 Conflict.
 *
 * WHY a custom exception instead of throwing RuntimeException directly?
 * 1. Semantics: The name tells you exactly WHAT went wrong
 * 2. @ResponseStatus: Declaratively maps this exception to an HTTP status code
 * 3. GlobalExceptionHandler can catch this specific type and return structured JSON
 * 4. Stacktrace is cleaner — no generic "RuntimeException: User already exists"
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
