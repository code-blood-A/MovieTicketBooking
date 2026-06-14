package com.moviebooking.movieservice.repository;

import com.moviebooking.movieservice.entity.Cast;
import com.moviebooking.movieservice.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CastRepository extends JpaRepository<Cast, Long> {

    List<Cast> findByMovie(Movie movie);

    void deleteByMovie(Movie movie);
}
