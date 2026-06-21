package com.moviebooking.showservice.repository;

import com.moviebooking.showservice.entity.ShowSeat;
import com.moviebooking.showservice.entity.enums.ShowSeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ShowSeatRepository — database queries for ShowSeat entities.
 *
 * Critical queries for the booking flow:
 *  1. Find a specific seat in a specific show (for lock/book operations)
 *  2. Count available seats (for "X seats left" display)
 *  3. Release expired locks (background job, future)
 *  4. Batch save (for show creation — saves 150+ seats at once)
 */
@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    /**
     * Find a specific seat in a specific show.
     * Used by Booking Service: "Lock seat E5 in show 42."
     */
    Optional<ShowSeat> findByShowIdAndSeatId(Long showId, Long seatId);

    /**
     * Get all seats for a show.
     * Used by: GET /api/shows/{showId}/seats to render the full seat map.
     */
    List<ShowSeat> findByShowId(Long showId);

    /**
     * Count available seats in a show.
     * Used by: "12 seats remaining" badge on the show listing.
     * More efficient than loading all seats and calling .size().
     */
    long countByShowIdAndStatus(Long showId, ShowSeatStatus status);

    /**
     * Get all seats of a specific status in a show.
     * Used by: Admin — "show me all locked seats that need review"
     */
    List<ShowSeat> findByShowIdAndStatus(Long showId, ShowSeatStatus status);

    /**
     * Release expired locks — finds LOCKED seats older than the given time.
     *
     * Future Enhancement: A scheduled @Scheduled job calls this every minute:
     *   LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
     *   releasedExpiredLocks(cutoff);
     *
     * @Modifying + @Query: Required for UPDATE/DELETE JPQL statements.
     *   @Modifying tells Spring Data this query mutates data (not a SELECT).
     *   Without it, Spring Data treats the query as read-only → exception.
     *   clearAutomatically = true: clears the JPA first-level cache after
     *   the bulk update, so subsequent reads see the updated data.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ShowSeat ss
            SET ss.status = com.moviebooking.showservice.entity.enums.ShowSeatStatus.AVAILABLE,
                ss.lockedByUserId = NULL,
                ss.lockedAt = NULL
            WHERE ss.status = com.moviebooking.showservice.entity.enums.ShowSeatStatus.LOCKED
              AND ss.lockedAt < :cutoffTime
            """)
    int releaseExpiredLocks(@Param("cutoffTime") LocalDateTime cutoffTime);
}
