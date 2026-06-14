package com.moviebooking.movieservice.dto;

import com.moviebooking.movieservice.entity.enums.CastRole;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CastResponse {
    private Long id;
    private String name;
    private CastRole role;
    private String characterName;
}
