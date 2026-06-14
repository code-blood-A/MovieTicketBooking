package com.moviebooking.movieservice.dto;

import com.moviebooking.movieservice.entity.enums.MovieStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * MovieRequest DTO — used for both CREATE and UPDATE operations.
 *
 * genreIds: client sends a list of existing genre IDs.
 * The service fetches Genre entities by ID and attaches them to the movie.
 * This avoids clients needing to send the full genre object.
 */
@Getter
@Setter
public class MovieRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String description;

    @NotBlank(message = "Language is required")
    private String language;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 600, message = "Duration cannot exceed 600 minutes")
    private Integer durationMinutes;

    private LocalDate releaseDate;

    private MovieStatus status;

    @DecimalMin(value = "0.0", message = "Rating must be between 0 and 10")
    @DecimalMax(value = "10.0", message = "Rating must be between 0 and 10")
    private Double rating;

    private String posterUrl;

    /** IDs of existing genres to attach to this movie */
    private List<Long> genreIds;

    /** Cast members to create along with this movie */
    @Valid
    private List<CastRequest> casts;
}
