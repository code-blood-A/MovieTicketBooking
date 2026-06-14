package com.moviebooking.movieservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class GenreResponse {
    private Long id;
    private String name;
}
