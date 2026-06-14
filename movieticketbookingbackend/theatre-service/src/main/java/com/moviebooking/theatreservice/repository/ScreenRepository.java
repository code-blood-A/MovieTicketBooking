package com.moviebooking.theatreservice.repository;

import com.moviebooking.theatreservice.entity.Screen;
import com.moviebooking.theatreservice.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {

    List<Screen> findByTheatre(Theatre theatre);

    List<Screen> findByTheatreId(Long theatreId);

    boolean existsByNameAndTheatreId(String name, Long theatreId);

    /** Fetch screen with all its seats in one query — used by Booking Service */
    @Query("SELECT s FROM Screen s LEFT JOIN FETCH s.seats WHERE s.id = :id")
    Optional<Screen> findByIdWithSeats(@Param("id") Long id);
}
