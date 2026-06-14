package com.moviebooking.movieservice.dto;

import com.moviebooking.movieservice.entity.enums.MovieStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MovieResponse DTO — the complete movie view returned to clients.
 *
 * Notice: contains List<GenreResponse> and List<CastResponse>, NOT entities.
 * This is the nested DTO pattern — responses can contain other response DTOs.
 * JSON result:
 * {
 *   "id": 1,
 *   "title": "3 Idiots",
 *   "genres": [{"id":1,"name":"Comedy"}, {"id":2,"name":"Drama"}],
 *   "casts": [{"id":1,"name":"Aamir Khan","role":"ACTOR","characterName":"Rancho"}],
 *   ...
 * }
 */
@Getter
@Builder
public class MovieResponse {

    private Long id;
    private String title;
    private String description;
    private String language;
    private Integer durationMinutes;
    private LocalDate releaseDate;
    private MovieStatus status;
    private Double rating;
    private String posterUrl;
    private List<GenreResponse> genres;
    private List<CastResponse> casts;
    private LocalDateTime createdAt;
}
