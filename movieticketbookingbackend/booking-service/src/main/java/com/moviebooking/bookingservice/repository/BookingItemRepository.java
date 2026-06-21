package com.moviebooking.bookingservice.repository;

import com.moviebooking.bookingservice.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BookingItemRepository — Spring Data JPA repository for BookingItem entities.
 */
@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    /**
     * Get all seat items for a booking.
     * Used when we need the seat list without loading the full Booking entity
     * (avoids lazy-loading issues in non-transactional contexts).
     */
    List<BookingItem> findByBookingId(Long bookingId);
}
