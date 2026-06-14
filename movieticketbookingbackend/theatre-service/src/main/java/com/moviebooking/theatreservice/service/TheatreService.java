package com.moviebooking.theatreservice.service;

import com.moviebooking.theatreservice.dto.*;

import java.util.List;

public interface TheatreService {

    // ── Theatre ──────────────────────────────────────────────
    TheatreResponse createTheatre(TheatreRequest request);
    TheatreResponse getTheatreById(Long id);
    List<TheatreResponse> getAllTheatres();
    List<TheatreResponse> getTheatresByCity(String city);
    TheatreResponse updateTheatre(Long id, TheatreRequest request);
    void deleteTheatre(Long id);

    // ── Screen ───────────────────────────────────────────────
    ScreenDetailResponse addScreen(Long theatreId, ScreenRequest request);
    ScreenDetailResponse getScreenById(Long screenId);
    List<ScreenSummaryResponse> getScreensByTheatre(Long theatreId);
}
