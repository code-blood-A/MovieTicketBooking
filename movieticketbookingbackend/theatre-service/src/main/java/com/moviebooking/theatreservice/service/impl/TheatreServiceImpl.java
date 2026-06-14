package com.moviebooking.theatreservice.service.impl;

import com.moviebooking.theatreservice.dto.*;
import com.moviebooking.theatreservice.entity.Screen;
import com.moviebooking.theatreservice.entity.Seat;
import com.moviebooking.theatreservice.entity.Theatre;
import com.moviebooking.theatreservice.entity.enums.ScreenType;
import com.moviebooking.theatreservice.entity.enums.SeatType;
import com.moviebooking.theatreservice.exception.ScreenNotFoundException;
import com.moviebooking.theatreservice.exception.TheatreNotFoundException;
import com.moviebooking.theatreservice.repository.ScreenRepository;
import com.moviebooking.theatreservice.repository.SeatRepository;
import com.moviebooking.theatreservice.repository.TheatreRepository;
import com.moviebooking.theatreservice.service.TheatreService;
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
public class TheatreServiceImpl implements TheatreService {

    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;

    // ═══════════════════════════════════════════════════════════
    // THEATRE OPERATIONS
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public TheatreResponse createTheatre(TheatreRequest request) {
        if (theatreRepository.existsByNameAndCity(request.getName(), request.getCity())) {
            throw new IllegalArgumentException(
                "Theatre '" + request.getName() + "' already exists in " + request.getCity());
        }

        Theatre theatre = Theatre.builder()
                .name(request.getName())
                .city(request.getCity())
                .address(request.getAddress())
                .pincode(request.getPincode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();

        Theatre saved = theatreRepository.save(theatre);
        log.info("Theatre created: {} in {} (id={})", saved.getName(), saved.getCity(), saved.getId());
        return toTheatreResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TheatreResponse getTheatreById(Long id) {
        Theatre theatre = theatreRepository.findByIdWithScreens(id)
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found with id: " + id));
        return toTheatreResponse(theatre);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TheatreResponse> getAllTheatres() {
        return theatreRepository.findAll()
                .stream().map(this::toTheatreResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TheatreResponse> getTheatresByCity(String city) {
        return theatreRepository.findByCityIgnoreCaseOrderByNameAsc(city)
                .stream().map(this::toTheatreResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TheatreResponse updateTheatre(Long id, TheatreRequest request) {
        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found with id: " + id));

        if (request.getName() != null)    theatre.setName(request.getName());
        if (request.getCity() != null)    theatre.setCity(request.getCity());
        if (request.getAddress() != null) theatre.setAddress(request.getAddress());
        if (request.getPincode() != null) theatre.setPincode(request.getPincode());
        if (request.getPhone() != null)   theatre.setPhone(request.getPhone());
        if (request.getEmail() != null)   theatre.setEmail(request.getEmail());

        return toTheatreResponse(theatreRepository.save(theatre));
    }

    @Override
    @Transactional
    public void deleteTheatre(Long id) {
        if (!theatreRepository.existsById(id)) {
            throw new TheatreNotFoundException("Theatre not found with id: " + id);
        }
        theatreRepository.deleteById(id);  // Cascades to screens and seats
        log.info("Theatre deleted: id={}", id);
    }

    // ═══════════════════════════════════════════════════════════
    // SCREEN OPERATIONS
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public ScreenDetailResponse addScreen(Long theatreId, ScreenRequest request) {
        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new TheatreNotFoundException("Theatre not found with id: " + theatreId));

        if (screenRepository.existsByNameAndTheatreId(request.getName(), theatreId)) {
            throw new IllegalArgumentException(
                "Screen '" + request.getName() + "' already exists in this theatre");
        }

        int totalSeats = request.getTotalRows() * request.getSeatsPerRow();

        Screen screen = Screen.builder()
                .name(request.getName())
                .screenType(request.getScreenType() != null ? request.getScreenType() : ScreenType.TWO_D)
                .totalRows(request.getTotalRows())
                .seatsPerRow(request.getSeatsPerRow())
                .totalSeats(totalSeats)
                .theatre(theatre)
                .build();

        Screen savedScreen = screenRepository.save(screen);

        // ═══════════════════════════════════════════════════════════
        // AUTO-GENERATE SEATS — The key design of this service
        // ═══════════════════════════════════════════════════════════
        // Factory-like pattern: service generates all seat entities
        // from a simple configuration (rows × seatsPerRow + seatType rules).
        List<Seat> seats = generateSeats(savedScreen, request);
        seatRepository.saveAll(seats);
        savedScreen.getSeats().addAll(seats);

        log.info("Screen '{}' created in Theatre {} with {} seats", 
                savedScreen.getName(), theatreId, totalSeats);
        return toScreenDetailResponse(savedScreen);
    }

    @Override
    @Transactional(readOnly = true)
    public ScreenDetailResponse getScreenById(Long screenId) {
        Screen screen = screenRepository.findByIdWithSeats(screenId)
                .orElseThrow(() -> new ScreenNotFoundException("Screen not found with id: " + screenId));
        return toScreenDetailResponse(screen);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenSummaryResponse> getScreensByTheatre(Long theatreId) {
        if (!theatreRepository.existsById(theatreId)) {
            throw new TheatreNotFoundException("Theatre not found with id: " + theatreId);
        }
        return screenRepository.findByTheatreId(theatreId)
                .stream().map(this::toScreenSummaryResponse).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // SEAT AUTO-GENERATION ALGORITHM
    // ═══════════════════════════════════════════════════════════

    /**
     * Generates all Seat entities for a newly created Screen.
     *
     * Algorithm:
     * - Row 0 → label "A", Row 1 → "B", ..., Row 25 → "Z"
     * - Each row has seatsPerRow seats: col 1, 2, 3, ...
     * - SeatType is determined per row via premiumRows/reclinerRows config
     *
     * Example: totalRows=10, seatsPerRow=15
     *   Rows A-G (7 rows) → REGULAR
     *   Rows H-I (2 rows) → PREMIUM
     *   Row J   (1 row)   → RECLINER
     *   Total: 150 seats
     *
     * LLD PATTERN: This is Template Method-like —
     * the overall algorithm (loop rows → loop cols → create seat) is fixed,
     * but the SeatType determination step is variable (delegated to determineSeatType).
     */
    private List<Seat> generateSeats(Screen screen, ScreenRequest request) {
        List<Seat> seats = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < request.getTotalRows(); rowIndex++) {
            // Convert 0-based index to letter: 0→'A', 1→'B', etc.
            String rowLabel = String.valueOf((char) ('A' + rowIndex));
            SeatType seatType = determineSeatType(rowLabel, request);

            for (int col = 1; col <= request.getSeatsPerRow(); col++) {
                String seatNumber = rowLabel + col;  // e.g., "A1", "B12"
                seats.add(Seat.builder()
                        .seatNumber(seatNumber)
                        .rowLabel(rowLabel)
                        .columnNumber(col)
                        .seatType(seatType)
                        .screen(screen)
                        .build());
            }
        }
        return seats;
    }

    /**
     * Determines the SeatType for a given row based on admin configuration.
     * Priority: RECLINER > PREMIUM > REGULAR (if a row is in both lists, RECLINER wins).
     */
    private SeatType determineSeatType(String rowLabel, ScreenRequest request) {
        if (request.getReclinerRows() != null && request.getReclinerRows().contains(rowLabel)) {
            return SeatType.RECLINER;
        }
        if (request.getPremiumRows() != null && request.getPremiumRows().contains(rowLabel)) {
            return SeatType.PREMIUM;
        }
        return SeatType.REGULAR;
    }

    // ═══════════════════════════════════════════════════════════
    // MAPPING METHODS (Entity → DTO)
    // ═══════════════════════════════════════════════════════════

    private TheatreResponse toTheatreResponse(Theatre theatre) {
        List<ScreenSummaryResponse> screenSummaries = theatre.getScreens() != null
                ? theatre.getScreens().stream().map(this::toScreenSummaryResponse).collect(Collectors.toList())
                : List.of();

        return TheatreResponse.builder()
                .id(theatre.getId())
                .name(theatre.getName())
                .city(theatre.getCity())
                .address(theatre.getAddress())
                .pincode(theatre.getPincode())
                .phone(theatre.getPhone())
                .email(theatre.getEmail())
                .totalScreens(screenSummaries.size())
                .screens(screenSummaries)
                .createdAt(theatre.getCreatedAt())
                .build();
    }

    private ScreenSummaryResponse toScreenSummaryResponse(Screen screen) {
        return ScreenSummaryResponse.builder()
                .id(screen.getId())
                .name(screen.getName())
                .screenType(screen.getScreenType())
                .totalSeats(screen.getTotalSeats())
                .build();
    }

    private ScreenDetailResponse toScreenDetailResponse(Screen screen) {
        List<SeatResponse> seatResponses = screen.getSeats() != null
                ? screen.getSeats().stream()
                        .map(s -> SeatResponse.builder()
                                .id(s.getId())
                                .seatNumber(s.getSeatNumber())
                                .rowLabel(s.getRowLabel())
                                .columnNumber(s.getColumnNumber())
                                .seatType(s.getSeatType())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        return ScreenDetailResponse.builder()
                .id(screen.getId())
                .name(screen.getName())
                .screenType(screen.getScreenType())
                .totalRows(screen.getTotalRows())
                .seatsPerRow(screen.getSeatsPerRow())
                .totalSeats(screen.getTotalSeats())
                .theatreId(screen.getTheatre() != null ? screen.getTheatre().getId() : null)
                .theatreName(screen.getTheatre() != null ? screen.getTheatre().getName() : null)
                .seats(seatResponses)
                .build();
    }
}
