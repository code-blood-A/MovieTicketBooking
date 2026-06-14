package com.moviebooking.theatreservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Theatre Service — manages Theatres, Screens, and Seats.
 * Port 8083. Registers as "theatre-service" in Eureka.
 */
@SpringBootApplication
public class TheatreServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TheatreServiceApplication.class, args);
    }
}
