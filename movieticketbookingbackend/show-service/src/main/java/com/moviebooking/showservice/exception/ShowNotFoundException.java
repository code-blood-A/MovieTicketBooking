package com.moviebooking.showservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a Show with the given ID does not exist in the database.
 * Maps to HTTP 404 Not Found automatically via @ResponseStatus.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ShowNotFoundException extends RuntimeException {
    public ShowNotFoundException(Long id) {
        super("Show not found with ID: " + id);
    }
}
