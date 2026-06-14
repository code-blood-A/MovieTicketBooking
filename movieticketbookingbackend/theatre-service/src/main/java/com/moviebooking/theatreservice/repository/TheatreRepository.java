package com.moviebooking.theatreservice.repository;

import com.moviebooking.theatreservice.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    List<Theatre> findByCityIgnoreCase(String city);

    List<Theatre> findByCityIgnoreCaseOrderByNameAsc(String city);

    boolean existsByNameAndCity(String name, String city);

    /** Load theatre with its screens in one query (avoids N+1 when listing screens) */
    @Query("SELECT DISTINCT t FROM Theatre t LEFT JOIN FETCH t.screens WHERE t.id = :id")
    Optional<Theatre> findByIdWithScreens(@Param("id") Long id);

    /** Search by city (case-insensitive partial match) */
    @Query("SELECT t FROM Theatre t WHERE LOWER(t.city) LIKE LOWER(CONCAT('%', :city, '%'))")
    List<Theatre> searchByCity(@Param("city") String city);
}
