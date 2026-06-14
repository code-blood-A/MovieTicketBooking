package com.moviebooking.movieservice.service;

import com.moviebooking.movieservice.dto.*;
import com.moviebooking.movieservice.entity.enums.MovieStatus;

import java.util.List;

public interface MovieService {

    // ── Genre Operations ──────────────────────────────────────
    GenreResponse createGenre(GenreRequest request);
    List<GenreResponse> getAllGenres();

    // ── Movie Operations ──────────────────────────────────────
    MovieResponse createMovie(MovieRequest request);
    MovieResponse getMovieById(Long id);
    List<MovieResponse> getAllMovies();
    List<MovieResponse> getMoviesByStatus(MovieStatus status);
    MovieResponse updateMovie(Long id, MovieRequest request);
    void deleteMovie(Long id);

    // ── Search ────────────────────────────────────────────────
    List<MovieResponse> searchMovies(String title, String language, MovieStatus status);
}
