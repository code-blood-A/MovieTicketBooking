package com.moviebooking.theatreservice.dto;

import com.moviebooking.theatreservice.entity.enums.ScreenType;
import lombok.Builder;
import lombok.Getter;

/** Lightweight screen info — used inside TheatreResponse list (no full seat data) */
@Getter @Builder
public class ScreenSummaryResponse {
    private Long id;
    private String name;
    private ScreenType screenType;
    private Integer totalSeats;
}
