package com.moviebooking.movieservice.service.impl;

import com.moviebooking.movieservice.dto.*;
import com.moviebooking.movieservice.entity.Cast;
import com.moviebooking.movieservice.entity.Genre;
import com.moviebooking.movieservice.entity.Movie;
import com.moviebooking.movieservice.entity.enums.MovieStatus;
import com.moviebooking.movieservice.exception.GenreNotFoundException;
import com.moviebooking.movieservice.exception.MovieNotFoundException;
import com.moviebooking.movieservice.repository.CastRepository;
import com.moviebooking.movieservice.repository.GenreRepository;
import com.moviebooking.movieservice.repository.MovieRepository;
import com.moviebooking.movieservice.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final CastRepository castRepository;

    // ═══════════════════════════════════════════════════════════
    // GENRE OPERATIONS
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public GenreResponse createGenre(GenreRequest request) {
        if (genreRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Genre '" + request.getName() + "' already exists");
        }
        Genre genre = Genre.builder().name(request.getName()).build();
        Genre saved = genreRepository.save(genre);
        log.info("Created genre: {} (id={})", saved.getName(), saved.getId());
        return toGenreResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findAll()
                .stream()
                .map(this::toGenreResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // MOVIE OPERATIONS
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        log.info("Creating movie: {}", request.getTitle());

        // Fetch genres by IDs (client sends genre IDs, not full genre objects)
        List<Genre> genres = new ArrayList<>();
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            genres = genreRepository.findByIdIn(request.getGenreIds());
            if (genres.size() != request.getGenreIds().size()) {
                throw new GenreNotFoundException("One or more genre IDs not found");
            }
        }

        // Build Movie entity
        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .language(request.getLanguage())
                .durationMinutes(request.getDurationMinutes())
                .releaseDate(request.getReleaseDate())
                .status(request.getStatus() != null ? request.getStatus() : MovieStatus.COMING_SOON)
                .rating(request.getRating() != null ? request.getRating() : 0.0)
                .posterUrl(request.getPosterUrl())
                .genres(genres)
                .build();

        Movie saved = movieRepository.save(movie);

        // Handle cast — build Cast entities linked to the saved movie
        if (request.getCasts() != null && !request.getCasts().isEmpty()) {
            List<Cast> casts = request.getCasts().stream()
                    .map(castReq -> Cast.builder()
                            .name(castReq.getName())
                            .role(castReq.getRole())
                            .characterName(castReq.getCharacterName())
                            .movie(saved) // Link to the saved movie
                            .build())
                    .collect(Collectors.toList());
            castRepository.saveAll(casts);
            saved.getCasts().addAll(casts);
        }

        log.info("Movie created: {} (id={})", saved.getTitle(), saved.getId());
        return toMovieResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        // Use JOIN FETCH query to load genres in single SQL query
        Movie movie = movieRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found with id: " + id));
        // Casts are loaded separately (can't JOIN FETCH two collections in one JPQL query)
        return toMovieResponse(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::toMovieResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesByStatus(MovieStatus status) {
        return movieRepository.findByStatus(status)
                .stream()
                .map(this::toMovieResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found with id: " + id));

        // Update only non-null fields (partial update)
        if (request.getTitle() != null)           movie.setTitle(request.getTitle());
        if (request.getDescription() != null)     movie.setDescription(request.getDescription());
        if (request.getLanguage() != null)        movie.setLanguage(request.getLanguage());
        if (request.getDurationMinutes() != null) movie.setDurationMinutes(request.getDurationMinutes());
        if (request.getReleaseDate() != null)     movie.setReleaseDate(request.getReleaseDate());
        if (request.getStatus() != null)          movie.setStatus(request.getStatus());
        if (request.getRating() != null)          movie.setRating(request.getRating());
        if (request.getPosterUrl() != null)       movie.setPosterUrl(request.getPosterUrl());

        if (request.getGenreIds() != null) {
            List<Genre> genres = genreRepository.findByIdIn(request.getGenreIds());
            movie.setGenres(genres);
        }

        Movie updated = movieRepository.save(movie);
        log.info("Movie updated: {} (id={})", updated.getTitle(), updated.getId());
        return toMovieResponse(updated);
    }

    @Override
    @Transactional
    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new MovieNotFoundException("Movie not found with id: " + id);
        }
        movieRepository.deleteById(id);
        log.info("Movie deleted: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> searchMovies(String title, String language, MovieStatus status) {
        return movieRepository.searchMovies(title, language, status)
                .stream()
                .map(this::toMovieResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE MAPPING METHODS (Entity → DTO)
    // ═══════════════════════════════════════════════════════════

    private MovieResponse toMovieResponse(Movie movie) {
        List<GenreResponse> genreResponses = movie.getGenres() != null
                ? movie.getGenres().stream().map(this::toGenreResponse).collect(Collectors.toList())
                : List.of();

        List<CastResponse> castResponses = movie.getCasts() != null
                ? movie.getCasts().stream().map(this::toCastResponse).collect(Collectors.toList())
                : List.of();

        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .language(movie.getLanguage())
                .durationMinutes(movie.getDurationMinutes())
                .releaseDate(movie.getReleaseDate())
                .status(movie.getStatus())
                .rating(movie.getRating())
                .posterUrl(movie.getPosterUrl())
                .genres(genreResponses)
                .casts(castResponses)
                .createdAt(movie.getCreatedAt())
                .build();
    }

    private GenreResponse toGenreResponse(Genre genre) {
        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getName())
                .build();
    }

    private CastResponse toCastResponse(Cast cast) {
        return CastResponse.builder()
                .id(cast.getId())
                .name(cast.getName())
                .role(cast.getRole())
                .characterName(cast.getCharacterName())
                .build();
    }
}
