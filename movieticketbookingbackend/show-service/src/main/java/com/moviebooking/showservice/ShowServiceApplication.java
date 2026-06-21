package com.moviebooking.showservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ShowServiceApplication — Entry point for Show Service.
 *
 * Port: 8084
 * Database: show_db
 *
 * This service bridges Movie Service and Theatre Service:
 *   - A Show says: "Movie X plays in Screen Y at 6:00 PM on 2024-12-25"
 *   - When a show is created, it auto-generates ShowSeat records
 *     (one per physical seat in the screen)
 *
 * HLD Note:
 * ┌─────────────┐    movieId    ┌──────────────┐    screenId   ┌────────────────┐
 * │ Movie       │ ─────────────▶│    Show      │◀─────────────  │ Theatre/Screen │
 * │ Service     │               │   Service    │               │ Service        │
 * └─────────────┘               └──────────────┘               └────────────────┘
 *                                      │
 *                                      │ generates
 *                                      ▼
 *                               ┌──────────────┐
 *                               │  ShowSeat    │ (AVAILABLE/LOCKED/BOOKED)
 *                               └──────────────┘
 *                                      │
 *                                      │ locked by
 *                                      ▼
 *                               ┌──────────────┐
 *                               │   Booking    │
 *                               │   Service    │
 *                               └──────────────┘
 */
@SpringBootApplication
public class ShowServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShowServiceApplication.class, args);
    }
}
