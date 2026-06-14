package com.moviebooking.theatreservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class TheatreResponse {
    private Long id;
    private String name;
    private String city;
    private String address;
    private String pincode;
    private String phone;
    private String email;
    private int totalScreens;
    private List<ScreenSummaryResponse> screens;
    private LocalDateTime createdAt;
}
