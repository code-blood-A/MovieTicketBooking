package com.moviebooking.movieservice.repository;

import com.moviebooking.movieservice.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    /** Find multiple genres by their IDs — used when creating a movie with genreIds */
    List<Genre> findByIdIn(List<Long> ids);
}
