package com.moviebooking.theatreservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TheatreNotFoundException extends RuntimeException {
    public TheatreNotFoundException(String message) { super(message); }
}
