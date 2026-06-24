package com.moviebooking.bookingservice.service.impl;

import com.moviebooking.bookingservice.client.ShowServiceClient;
import com.moviebooking.bookingservice.dto.*;
import com.moviebooking.bookingservice.entity.Booking;
import com.moviebooking.bookingservice.entity.BookingItem;
import com.moviebooking.bookingservice.entity.enums.BookingStatus;
import com.moviebooking.bookingservice.exception.BookingNotFoundException;
import com.moviebooking.bookingservice.exception.SeatLockException;
import com.moviebooking.bookingservice.repository.BookingRepository;
import com.moviebooking.bookingservice.service.BookingService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BookingServiceImpl — the core orchestration logic for the booking flow.
 *
 * ═══════════════════════════════════════════════════════════════════
 * BOOKING FLOW (the most important thing to understand in this class)
 * ═══════════════════════════════════════════════════════════════════
 *
 * createBooking():
 *   1. For each seatId in request:
 *      → Feign: PUT /api/shows/{showId}/seats/{seatId}/status → LOCKED
 *      → If seat already locked by someone else → FeignException (409)
 *      → Track successfully locked seats in case we need to rollback
 *   2. If ANY seat lock fails:
 *      → Release all previously locked seats (AVAILABLE) — compensation
 *      → Throw SeatLockException — client sees which seat was unavailable
 *   3. All seats locked successfully:
 *      → Build Booking (PENDING) + BookingItems from Feign responses
 *      → Calculate totalAmount = sum of all seat prices
 *      → Save to DB in one @Transactional commit
 *      → Return BookingResponse with PENDING status
 *
 * confirmBooking():
 *   1. Load Booking — must be in PENDING state
 *   2. For each BookingItem:
 *      → Feign: PUT → BOOKED
 *   3. Booking.status = CONFIRMED
 *   4. Save and return
 *
 * cancelBooking():
 *   1. Load Booking — must be PENDING or CONFIRMED
 *   2. For each BookingItem:
 *      → Feign: PUT → AVAILABLE (releases the lock)
 *   3. Booking.status = CANCELLED
 *   4. Save and return
 *
 * ═══════════════════════════════════════════════════════════════════
 * PARTIAL FAILURE HANDLING (the hardest part of distributed systems)
 * ═══════════════════════════════════════════════════════════════════
 *
 * Scenario: User selects 3 seats [A1, B2, C3].
 *   Step 1: Lock A1 → SUCCESS (locked)
 *   Step 2: Lock B2 → SUCCESS (locked)
 *   Step 3: Lock C3 → FAIL (already locked by User 42)
 *
 * Without compensation:
 *   A1 and B2 are permanently LOCKED. No booking was created.
 *   User can never book those seats again — "ghost locks".
 *
 * With our compensation (rollback):
 *   On C3 failure → loop back through [A1, B2] and release them.
 *   A1 → AVAILABLE, B2 → AVAILABLE.
 *   Now other users can book A1 and B2.
 *   User sees error: "Seat C3 is no longer available. Please re-select."
 *
 * This is the SAGA pattern (without a framework):
 *   Forward actions: LOCK each seat.
 *   Compensating actions: RELEASE each locked seat on failure.
 *
 * In production systems (Netflix, Uber, BookMyShow), this would use:
 *   - Distributed Sagas with an orchestrator (Temporal, Camunda)
 *   - Or choreography-based Sagas with Kafka events
 *   For MVP, our simple loop is correct and sufficient.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ShowServiceClient showServiceClient;

    // ─────────────────────────────────────────────────────────────────
    // CREATE BOOKING
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, Long userId) {
        log.info("Creating booking: userId={}, showId={}, seats={}",
                userId, request.getShowId(), request.getSeatIds());

        // ── Step 1: Lock all seats via Show Service ───────────────────────
        // We track (seatId → SeatResponse) for locked seats.
        // If any lock fails, we need this list to release the already-locked ones.
        List<ShowSeatResponse> lockedSeats = new ArrayList<>();

        for (Long seatId : request.getSeatIds()) {
            try {
                ShowSeatStatusRequest lockRequest =
                        new ShowSeatStatusRequest("LOCKED", userId);

                ShowSeatResponse seatResponse = showServiceClient.updateSeatStatus(
                        request.getShowId(), seatId, lockRequest, userId);

                lockedSeats.add(seatResponse);
                log.debug("Locked seat {} for show {}", seatId, request.getShowId());

            } catch (FeignException.Conflict ex) {
                // 409 Conflict from Show Service = seat is already LOCKED or BOOKED
                log.warn("Seat {} is already taken. Rolling back {} locks.",
                        seatId, lockedSeats.size());

                // ── Compensation: release all already-locked seats ────────
                releaseLockedSeats(request.getShowId(), lockedSeats, userId);

                throw new SeatLockException(
                        "Seat is not available. Please select a different seat. " +
                        "Already released " + lockedSeats.size() + " previously selected seat(s).");

            } catch (FeignException.NotFound ex) {
                // 404 from Show Service = showId or seatId doesn't exist.
                // This is a client error (bad input), not a network/infra error.
                // No compensation needed — no seats were successfully locked before this
                // (if seats WERE locked before a bad seatId, release them first).
                log.warn("Show or Seat not found: showId={}, seatId={}. Rolling back {} locks.",
                        request.getShowId(), seatId, lockedSeats.size());

                releaseLockedSeats(request.getShowId(), lockedSeats, userId);

                throw new IllegalArgumentException(
                        "Show ID " + request.getShowId() + " or Seat ID " + seatId +
                        " does not exist. Please verify your show and seat selection.");

            } catch (FeignException ex) {
                // Other Feign errors (Show Service down, network timeout, 5xx)
                log.error("Feign error locking seat {}: {} {}", seatId, ex.status(), ex.getMessage());

                // Release already-locked seats before propagating error
                releaseLockedSeats(request.getShowId(), lockedSeats, userId);

                throw new RuntimeException(
                        "Failed to communicate with Show Service. Please try again. " +
                        "Released " + lockedSeats.size() + " seat lock(s).");
            }
        }

        // ── Step 2: Build Booking + BookingItems ──────────────────────────
        // All seats locked successfully — now persist the booking.

        // Calculate total: sum of each seat's price (fetched from Show Service)
        BigDecimal totalAmount = lockedSeats.stream()
                .map(ShowSeatResponse::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Booking booking = Booking.builder()
                .userId(userId)
                .showId(request.getShowId())
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .build();

        // Build one BookingItem per locked seat (snapshot pattern)
        List<BookingItem> items = lockedSeats.stream()
                .map(seat -> BookingItem.builder()
                        .booking(booking)
                        .showSeatId(seat.getId())       // ShowSeat PK from show_db
                        .seatLabel(seat.getSeatLabel())  // e.g., "E5" — snapshot
                        .seatType(seat.getSeatType())    // REGULAR/PREMIUM/RECLINER
                        .price(seat.getPrice())          // price at booking time
                        .build())
                .collect(Collectors.toList());

        booking.getItems().addAll(items);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created: id={}, status=PENDING, total={}",
                savedBooking.getId(), totalAmount);

        return toBookingResponse(savedBooking);
    }

    // ─────────────────────────────────────────────────────────────────
    // READ OPERATIONS
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        return toBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toBookingResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // CONFIRM BOOKING (simulates payment success)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Confirms a PENDING booking:
     *   LOCKED seats → BOOKED (permanent, no going back)
     *   Booking status → CONFIRMED
     *
     * In production, this would be triggered by Payment Service
     * via a webhook after the payment gateway processes the charge.
     * For MVP, we call this endpoint directly to simulate payment success.
     */
    @Override
    @Transactional
    public BookingResponse confirmBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Booking " + bookingId + " is not in PENDING state (current: " +
                    booking.getStatus() + "). Cannot confirm.");
        }

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalStateException("You are not authorized to confirm this booking.");
        }

        log.info("Confirming booking {}: marking {} seats as BOOKED", bookingId, booking.getItems().size());

        // Mark each seat as BOOKED in Show Service
        for (BookingItem item : booking.getItems()) {
            try {
                showServiceClient.updateSeatStatus(
                        booking.getShowId(),
                        item.getShowSeatId(),
                        new ShowSeatStatusRequest("BOOKED", userId),
                        userId);
            } catch (FeignException ex) {
                // If BOOK fails (e.g., Show Service down), mark booking as FAILED
                // Seats stay LOCKED — need manual intervention or retry
                log.error("Failed to BOOK seat {} for booking {}: {}",
                        item.getShowSeatId(), bookingId, ex.getMessage());
                booking.setStatus(BookingStatus.FAILED);
                bookingRepository.save(booking);
                throw new RuntimeException("Payment processed but seat confirmation failed. " +
                        "Booking marked FAILED — contact support with booking ID: " + bookingId);
            }
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmed = bookingRepository.save(booking);
        log.info("Booking {} confirmed successfully", bookingId);
        return toBookingResponse(confirmed);
    }

    // ─────────────────────────────────────────────────────────────────
    // CANCEL BOOKING
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking " + bookingId + " is already cancelled.");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Confirmed bookings cannot be cancelled directly. Contact support for refund.");
        }

        log.info("Cancelling booking {}: releasing {} seat locks", bookingId, booking.getItems().size());

        // Release all LOCKED seats back to AVAILABLE
        for (BookingItem item : booking.getItems()) {
            try {
                showServiceClient.updateSeatStatus(
                        booking.getShowId(),
                        item.getShowSeatId(),
                        new ShowSeatStatusRequest("AVAILABLE", userId),
                        userId);
            } catch (FeignException ex) {
                // Log but continue — we want to cancel the booking even if
                // one seat release fails (ghost lock, Show Service restarted etc.)
                log.error("Failed to release seat {} during cancellation: {}",
                        item.getShowSeatId(), ex.getMessage());
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelled = bookingRepository.save(booking);
        log.info("Booking {} cancelled", bookingId);
        return toBookingResponse(cancelled);
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Compensation action: release all seats that were successfully locked
     * before a failure occurred.
     *
     * WHY best-effort (no exception on release failure)?
     * If releasing A1 also fails (Show Service went down mid-flight),
     * throwing an exception here would mask the original SeatLockException.
     * We log the failure and continue — a cleanup job or admin action
     * can handle ghost locks later. The user still sees the correct error
     * ("Seat C3 not available") rather than a confusing "release failed" error.
     */
    private void releaseLockedSeats(Long showId, List<ShowSeatResponse> lockedSeats, Long userId) {
        for (ShowSeatResponse seat : lockedSeats) {
            try {
                showServiceClient.updateSeatStatus(
                        showId,
                        seat.getId(),
                        new ShowSeatStatusRequest("AVAILABLE", userId),
                        userId);
                log.debug("Released seat {} (compensation)", seat.getId());
            } catch (FeignException ex) {
                // Ghost lock — will be cleaned by TTL scheduler (future feature)
                log.error("Failed to release seat {} during compensation: {}",
                        seat.getId(), ex.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // MAPPING HELPERS
    // ─────────────────────────────────────────────────────────────────

    private BookingResponse toBookingResponse(Booking booking) {
        List<BookingItemResponse> itemResponses = booking.getItems().stream()
                .map(item -> BookingItemResponse.builder()
                        .id(item.getId())
                        .showSeatId(item.getShowSeatId())
                        .seatLabel(item.getSeatLabel())
                        .seatType(item.getSeatType())
                        .price(item.getPrice())
                        .build())
                .toList();

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .items(itemResponses)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
