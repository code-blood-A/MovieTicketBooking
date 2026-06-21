package com.moviebooking.showservice.service.impl;

import com.moviebooking.showservice.dto.*;
import com.moviebooking.showservice.entity.Show;
import com.moviebooking.showservice.entity.ShowSeat;
import com.moviebooking.showservice.entity.enums.ShowSeatStatus;
import com.moviebooking.showservice.entity.enums.ShowStatus;
import com.moviebooking.showservice.exception.ShowNotFoundException;
import com.moviebooking.showservice.exception.ShowSeatNotFoundException;
import com.moviebooking.showservice.repository.ShowRepository;
import com.moviebooking.showservice.repository.ShowSeatRepository;
import com.moviebooking.showservice.service.ShowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ShowServiceImpl — core business logic for Show Service.
 *
 * Key operations:
 * 1. createShow() — creates Show + auto-generates ShowSeat records
 * 2. searchShows() — flexible search with optional filters
 * 3. updateSeatStatus() — handles AVAILABLE → LOCKED → BOOKED transitions
 *
 * @Transactional: Database operations are wrapped in a transaction.
 *                 If ANY step fails, the ENTIRE operation is rolled back.
 *                 Example: createShow() inserts Show + 150 ShowSeats.
 *                 If the 100th seat insert fails, the Show record is also
 *                 rolled back.
 *                 Without @Transactional, you'd have a partial show in the DB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowServiceImpl implements ShowService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

    /**
     * Seat type price multipliers.
     * REGULAR = 1.0× base price
     * PREMIUM = 1.5× base price
     * RECLINER = 2.0× base price
     */
    private static final BigDecimal PREMIUM_MULTIPLIER = new BigDecimal("1.5");
    private static final BigDecimal RECLINER_MULTIPLIER = new BigDecimal("2.0");

    // ─────────────────────────────────────────────────────────────────
    // CREATE SHOW
    // ─────────────────────────────────────────────────────────────────

    /**
     * Creates a Show and auto-generates ShowSeat records for every seat in the
     * screen.
     *
     * HOW seat generation works:
     * ──────────────────────────
     * Rows go from A to (A + totalRows - 1).
     * For totalRows=10: rows are A, B, C, D, E, F, G, H, I, J.
     *
     * For each row, we create seatsPerRow seats numbered 1..N.
     * Seat label = row letter + seat number (e.g., "E5", "J12").
     *
     * Each seat gets a type based on the premiumRows/reclinerRows lists.
     * Price = basePrice × multiplier for that seat type.
     *
     * This is the SAME logic as Theatre Service's screen seat generator,
     * but here we create ShowSeat records (with status AVAILABLE) instead of
     * Theatre Service's Seat records.
     *
     * Performance:
     * - For a 10×15 screen: 150 ShowSeat inserts.
     * - saveAll() with batch_size=50 → 3 DB round trips instead of 150.
     * - @Transactional ensures all-or-nothing.
     */
    @Override
    @Transactional
    public ShowResponse createShow(ShowRequest request) {
        System.out.println("aakash");
        log.info("Creating show: movieId={}, screenId={}, date={}, time={}",
                request.getMovieId(), request.getScreenId(),
                request.getShowDate(), request.getShowTime());

        int totalSeats = request.getTotalRows() * request.getSeatsPerRow();

        // Step 1: Build and save the Show entity
        Show show = Show.builder()
                .movieId(request.getMovieId())
                .screenId(request.getScreenId())
                .showDate(request.getShowDate())
                .showTime(request.getShowTime())
                .basePrice(request.getBasePrice())
                .durationMinutes(request.getDurationMinutes())
                .language(request.getLanguage())
                .status(ShowStatus.SCHEDULED)
                .totalSeats(totalSeats)
                .availableSeats(totalSeats) // all seats available initially
                .build();

        show = showRepository.save(show);
        log.debug("Saved show with ID={}", show.getId());

        // Step 2: Generate all ShowSeat records
        List<ShowSeat> showSeats = generateShowSeats(show, request);

        // Step 3: Batch save all seats (leverages hibernate.jdbc.batch_size=50)
        showSeatRepository.saveAll(showSeats);
        log.info("Generated {} ShowSeat records for showId={}", showSeats.size(), show.getId());

        return toShowResponse(show);
    }

    /**
     * Generates ShowSeat records based on the seat layout provided in ShowRequest.
     *
     * Row letter: (char)('A' + rowIndex) → rowIndex=0 → 'A', rowIndex=7 → 'H'
     * Seat label: row letter + seat number → "A1", "H3", "J15"
     *
     * Why Set for premiumRows/reclinerRows lookups?
     * → O(1) lookup. If admin provides 3 premium rows, Set.contains() is faster
     * than List.contains() for frequent lookups inside nested loops.
     */
    private List<ShowSeat> generateShowSeats(Show show, ShowRequest request) {
        // Convert to uppercase sets for case-insensitive matching
        Set<String> premiumRowSet = request.getPremiumRows() == null
                ? Set.of()
                : request.getPremiumRows().stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());

        Set<String> reclinerRowSet = request.getReclinerRows() == null
                ? Set.of()
                : request.getReclinerRows().stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());

        List<ShowSeat> seats = new ArrayList<>(request.getTotalRows() * request.getSeatsPerRow());

        for (int rowIndex = 0; rowIndex < request.getTotalRows(); rowIndex++) {
            // Convert row index to letter: 0→A, 1→B, ..., 9→J, ..., 25→Z
            String rowLabel = String.valueOf((char) ('A' + rowIndex));

            // Determine seat type for this entire row
            String seatType;
            BigDecimal price;

            if (reclinerRowSet.contains(rowLabel)) {
                seatType = "RECLINER";
                price = request.getBasePrice().multiply(RECLINER_MULTIPLIER);
            } else if (premiumRowSet.contains(rowLabel)) {
                seatType = "PREMIUM";
                price = request.getBasePrice().multiply(PREMIUM_MULTIPLIER);
            } else {
                seatType = "REGULAR";
                price = request.getBasePrice();
            }

            // Create one ShowSeat per seat in this row
            for (int seatNum = 1; seatNum <= request.getSeatsPerRow(); seatNum++) {
                String seatLabel = rowLabel + seatNum; // e.g., "A1", "J15"

                ShowSeat showSeat = ShowSeat.builder()
                        .show(show)
                        .seatId(null) // No direct FK to Theatre's seat — MVP simplification
                        .seatLabel(seatLabel)
                        .seatType(seatType)
                        .price(price)
                        .status(ShowSeatStatus.AVAILABLE)
                        .build();

                seats.add(showSeat);
            }
        }

        return seats;
    }

    // ─────────────────────────────────────────────────────────────────
    // READ OPERATIONS
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ShowNotFoundException(id));
        return toShowResponse(show);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> searchShows(Long movieId, Long screenId, LocalDate showDate,
            String language, ShowStatus status) {
        return showRepository.searchShows(movieId, screenId, showDate, language, status)
                .stream()
                .map(this::toShowResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByMovieAndDate(Long movieId, LocalDate date) {
        return showRepository.findByMovieIdAndShowDateOrderByShowTimeAsc(movieId, date)
                .stream()
                .map(this::toShowResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // UPDATE OPERATIONS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Updates the status of a Show (admin operation).
     *
     * Valid transitions:
     * SCHEDULED → OPEN_FOR_BOOKING (tickets go on sale)
     * OPEN_FOR_BOOKING → ONGOING (show starts)
     * ONGOING → COMPLETED (show ends)
     * Any state → CANCELLED (admin cancels)
     *
     * We don't enforce strict state transitions here (MVP).
     * In production, add a state machine to prevent invalid transitions
     * like COMPLETED → SCHEDULED.
     */
    @Override
    @Transactional
    public ShowResponse updateShowStatus(Long id, ShowStatus newStatus) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ShowNotFoundException(id));

        log.info("Updating show {} status: {} → {}", id, show.getStatus(), newStatus);
        show.setStatus(newStatus);
        return toShowResponse(showRepository.save(show));
    }

    @Override
    @Transactional
    public void deleteShow(Long id) {
        if (!showRepository.existsById(id)) {
            throw new ShowNotFoundException(id);
        }
        showRepository.deleteById(id);
        log.info("Deleted show with ID={}", id);
    }

    // ─────────────────────────────────────────────────────────────────
    // SEAT OPERATIONS
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> getSeatsByShow(Long showId) {
        // Verify show exists first
        if (!showRepository.existsById(showId)) {
            throw new ShowNotFoundException(showId);
        }
        return showSeatRepository.findByShowId(showId)
                .stream()
                .map(this::toShowSeatResponse)
                .toList();
    }

    /**
     * Updates the status of a specific seat in a show.
     *
     * LOCK flow (called by Booking Service when user selects seat):
     * 1. Check current status is AVAILABLE → if not, throw conflict
     * 2. Set status = LOCKED, lockedByUserId = userId, lockedAt = now
     * 3. Decrement show.availableSeats by 1
     *
     * BOOK flow (called by Booking Service after payment confirmed):
     * 1. Check current status is LOCKED and lockedByUserId matches
     * 2. Set status = BOOKED — no seat count change (already decremented)
     *
     * RELEASE flow (payment failed/abandoned):
     * 1. Set status = AVAILABLE, clear lock fields
     * 2. Increment show.availableSeats by 1
     *
     * @Transactional: ShowSeat update + Show.availableSeats update must be atomic.
     */
    @Override
    @Transactional
    public ShowSeatResponse updateSeatStatus(Long showId, Long seatId,
            SeatStatusUpdateRequest request) {
        // ShowSeat.id is the PK of show_seats table.
        // The URL path variable {seatId} = ShowSeat.id, NOT the theatre seat_id
        // reference.
        ShowSeat seat = showSeatRepository.findById(seatId)
                .filter(s -> s.getShow().getId().equals(showId)) // ensure it belongs to this show
                .orElseThrow(() -> new ShowSeatNotFoundException(showId, seatId));

        ShowSeatStatus newStatus = request.getStatus();
        ShowSeatStatus currentStatus = seat.getStatus();

        log.info("Seat {} in show {}: {} → {}", seatId, showId, currentStatus, newStatus);

        switch (newStatus) {
            case LOCKED -> {
                if (currentStatus != ShowSeatStatus.AVAILABLE) {
                    throw new IllegalStateException(
                            "Seat " + seat.getSeatLabel() + " is not available (current status: " + currentStatus
                                    + ")");
                }
                seat.setStatus(ShowSeatStatus.LOCKED);
                seat.setLockedByUserId(request.getUserId());
                seat.setLockedAt(LocalDateTime.now());

                // Decrement available seats on the parent Show
                updateAvailableSeatsCount(showId, -1);
            }
            case BOOKED -> {
                if (currentStatus != ShowSeatStatus.LOCKED) {
                    throw new IllegalStateException(
                            "Seat " + seat.getSeatLabel() + " must be LOCKED before booking (current: " + currentStatus
                                    + ")");
                }
                // Optionally validate lockedByUserId matches request.getUserId()
                seat.setStatus(ShowSeatStatus.BOOKED);
                // Keep lockedByUserId for audit trail
            }
            case AVAILABLE -> {
                // Releasing a lock (payment failed or user cancelled)
                seat.setStatus(ShowSeatStatus.AVAILABLE);
                seat.setLockedByUserId(null);
                seat.setLockedAt(null);

                // Only increment if we're releasing from LOCKED (not BOOKED)
                if (currentStatus == ShowSeatStatus.LOCKED) {
                    updateAvailableSeatsCount(showId, +1);
                }
            }
        }

        return toShowSeatResponse(showSeatRepository.save(seat));
    }

    /**
     * Updates the availableSeats counter on the Show entity.
     * Called as part of seat lock/release operations.
     *
     * @param delta +1 to increment (seat released), -1 to decrement (seat locked)
     */
    private void updateAvailableSeatsCount(Long showId, int delta) {
        showRepository.findById(showId).ifPresent(show -> {
            int updated = show.getAvailableSeats() + delta;
            show.setAvailableSeats(Math.max(0, updated)); // never go negative
            showRepository.save(show);
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // MAPPING HELPERS
    // ─────────────────────────────────────────────────────────────────

    private ShowResponse toShowResponse(Show show) {
        return ShowResponse.builder()
                .id(show.getId())
                .movieId(show.getMovieId())
                .screenId(show.getScreenId())
                .showDate(show.getShowDate())
                .showTime(show.getShowTime())
                .basePrice(show.getBasePrice())
                .durationMinutes(show.getDurationMinutes())
                .language(show.getLanguage())
                .status(show.getStatus())
                .totalSeats(show.getTotalSeats())
                .availableSeats(show.getAvailableSeats())
                .createdAt(show.getCreatedAt())
                .build();
    }

    private ShowSeatResponse toShowSeatResponse(ShowSeat seat) {
        return ShowSeatResponse.builder()
                .id(seat.getId())
                .seatId(seat.getSeatId())
                .seatLabel(seat.getSeatLabel())
                .seatType(seat.getSeatType())
                .price(seat.getPrice())
                .status(seat.getStatus())
                .build();
    }
}
