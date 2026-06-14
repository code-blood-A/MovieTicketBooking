package com.moviebooking.movieservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Movie Service — Manages movies, genres, and cast.
 * Owns movie_db. Registers with Eureka as "movie-service".
 * Gateway routes /api/movies/** → lb://movie-service.
 */
@SpringBootApplication
public class MovieServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MovieServiceApplication.class, args);
    }
}
