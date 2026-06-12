package com.moviebooking.userservice.controller;

import com.moviebooking.userservice.dto.AuthResponse;
import com.moviebooking.userservice.dto.LoginRequest;
import com.moviebooking.userservice.dto.RegisterRequest;
import com.moviebooking.userservice.dto.UserProfileResponse;
import com.moviebooking.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * UserController — HTTP layer. Thin adapter between HTTP and business logic.
 *
 * ═══════════════════════════════════════════════════════════
 * THE CONTROLLER SHOULD DO NOTHING EXCEPT:
 * ═══════════════════════════════════════════════════════════
 * 1. Receive the HTTP request
 * 2. Validate input (@Valid)
 * 3. Delegate to the service layer
 * 4. Wrap the result in a ResponseEntity with the right HTTP status
 * 5. Return the response
 *
 * NO business logic here. No DB queries. No JWT parsing. No if-else chains.
 * If your controller method exceeds ~10 lines, you're doing too much.
 *
 * WHY this discipline?
 * The controller is the hardest layer to unit test (requires mocking HTTP context).
 * Keeping it thin means all business logic is in easily-testable service classes.
 *
 * ═══════════════════════════════════════════════════════════
 * HTTP STATUS CODES — WHY EACH ONE
 * ═══════════════════════════════════════════════════════════
 * 201 CREATED  → register: a new resource (user) was created
 * 200 OK       → login, get profile: read/action succeeded, no new resource
 * 400 BAD REQ  → validation failed (handled by GlobalExceptionHandler)
 * 401 UNAUTHORIZED → wrong password (handled by GlobalExceptionHandler)
 * 404 NOT FOUND    → user not found (handled by GlobalExceptionHandler)
 * 409 CONFLICT     → email already taken (handled by GlobalExceptionHandler)
 *
 * ═══════════════════════════════════════════════════════════
 * @RequestMapping vs @GetMapping / @PostMapping
 * ═══════════════════════════════════════════════════════════
 * @RequestMapping("/api/users") on the class: sets the base URL prefix
 * @PostMapping("/register")     on the method: full URL = /api/users/register
 * This keeps the URL structure clear and avoids repetition.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * POST /api/users/register
     *
     * Public endpoint — no JWT required (whitelisted in Gateway's JwtAuthenticationFilter).
     *
     * @Valid triggers Bean Validation on RegisterRequest fields.
     * If validation fails → MethodArgumentNotValidException → GlobalExceptionHandler → 400
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/users/register — email: {}", request.getEmail());
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created
    }

    /**
     * POST /api/users/login
     *
     * Public endpoint — no JWT required.
     * Returns JWT token on success. Client stores this and sends it in future requests.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/users/login — email: {}", request.getEmail());
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response); // 200 OK
    }

    /**
     * GET /api/users/profile
     *
     * PROTECTED endpoint — requires JWT (Gateway validates it).
     *
     * KEY CONCEPT: @RequestHeader("X-User-Id")
     * The Gateway validates the JWT and adds the X-User-Id header to the request
     * before forwarding it here. So we READ the userId from the header —
     * we NEVER parse JWT in this service.
     *
     * This is the "Gateway injects identity" pattern:
     * Client → [JWT in Authorization header] → Gateway → [X-User-Id header] → User Service
     *
     * If someone calls this endpoint directly (bypassing Gateway) without X-User-Id,
     * Spring returns 400 Bad Request (missing required header). In production,
     * direct access is blocked at the network level anyway.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/users/profile — userId: {}", userId);
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response); // 200 OK
    }

    /**
     * GET /api/users/health
     *
     * Simple health check — useful for testing the service is up.
     * (Actuator's /actuator/health is the "real" health endpoint for monitoring)
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is UP ✓");
    }
}
