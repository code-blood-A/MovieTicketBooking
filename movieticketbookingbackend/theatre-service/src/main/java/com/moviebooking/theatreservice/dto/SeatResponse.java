package com.moviebooking.theatreservice.dto;

import com.moviebooking.theatreservice.entity.enums.SeatType;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class SeatResponse {
    private Long id;
    private String seatNumber;   // "A1", "B12", "J5"
    private String rowLabel;     // "A", "B", "J"
    private Integer columnNumber;
    private SeatType seatType;   // REGULAR, PREMIUM, RECLINER
}
