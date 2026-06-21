package com.moviebooking.bookingservice.repository;

import com.moviebooking.bookingservice.entity.Booking;
import com.moviebooking.bookingservice.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BookingRepository — Spring Data JPA repository for Booking entities.
 *
 * Spring Data JPA generates SQL from method names at compile time.
 * No SQL needed for standard CRUD and simple queries.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Find all bookings made by a specific user.
     * Used by: GET /api/bookings/my → user's booking history screen.
     * Sorted newest first (latest booking at top of list).
     */
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all bookings for a specific show.
     * Used by: Admin panel → "Who booked show 42?"
     */
    List<Booking> findByShowId(Long showId);

    /**
     * Find bookings by user and status.
     * Used by: "My active bookings" (CONFIRMED only) or "My pending payments".
     * Example: findByUserIdAndStatus(userId, BookingStatus.CONFIRMED)
     */
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);
}
