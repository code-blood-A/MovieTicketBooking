package com.moviebooking.theatreservice.repository;

import com.moviebooking.theatreservice.entity.Seat;
import com.moviebooking.theatreservice.entity.enums.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByScreenIdOrderByRowLabelAscColumnNumberAsc(Long screenId);

    List<Seat> findByScreenIdAndSeatType(Long screenId, SeatType seatType);

    int countByScreenId(Long screenId);

    /** Batch fetch by IDs — used by Booking Service to validate seat selections */
    List<Seat> findByIdIn(List<Long> ids);
}
