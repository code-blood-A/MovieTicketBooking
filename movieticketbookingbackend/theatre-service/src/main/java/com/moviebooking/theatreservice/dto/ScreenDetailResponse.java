package com.moviebooking.theatreservice.dto;

import com.moviebooking.theatreservice.entity.enums.ScreenType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Full screen detail with all seat data — returned by GET /api/theatres/screens/{id} */
@Getter @Builder
public class ScreenDetailResponse {
    private Long id;
    private String name;
    private ScreenType screenType;
    private Integer totalRows;
    private Integer seatsPerRow;
    private Integer totalSeats;
    private Long theatreId;
    private String theatreName;
    private List<SeatResponse> seats;
}
