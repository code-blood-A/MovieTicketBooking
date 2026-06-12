package com.moviebooking.userservice.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * AuthResponse DTO — returned after successful registration or login.
 *
 * ═══════════════════════════════════════════════════════════
 * LLD PATTERN: Builder Pattern
 * ═══════════════════════════════════════════════════════════
 * Problem: AuthResponse has 7 fields. A constructor call looks like:
 *   new AuthResponse(token, "Bearer", 42L, "user@mail.com", "John", "ROLE_USER", 86400000L)
 * It's impossible to tell which argument is which at the call site.
 * Easy to swap userId and expiresIn — compiler won't catch it (both are Long).
 *
 * Builder Pattern solution:
 *   AuthResponse.builder()
 *       .accessToken(token)
 *       .tokenType("Bearer")
 *       .userId(user.getId())
 *       .email(user.getEmail())
 *       .name(user.getName())
 *       .role(user.getRole().name())
 *       .expiresIn(86400000L)
 *       .build();
 *
 * Every field is NAMED at the call site → self-documenting, no position errors.
 * This is exactly how Lombok's @Builder works — it generates the builder class.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY @Getter but no @Setter?
 * ═══════════════════════════════════════════════════════════
 * Response objects are IMMUTABLE by design:
 * - Built once using Builder
 * - Serialized to JSON by Jackson (uses getters, not setters)
 * - Never modified after creation
 * Removing @Setter enforces this at compile time.
 *
 * ═══════════════════════════════════════════════════════════
 * WHAT THE CLIENT RECEIVES (JSON)
 * ═══════════════════════════════════════════════════════════
 * {
 *   "accessToken": "eyJhbGc...",
 *   "tokenType": "Bearer",
 *   "userId": 42,
 *   "email": "john@example.com",
 *   "name": "John Doe",
 *   "role": "ROLE_USER",
 *   "expiresIn": 86400000
 * }
 *
 * The client stores accessToken and sends it in every subsequent request:
 *   Authorization: Bearer eyJhbGc...
 */
@Getter
@Builder
public class AuthResponse {

    /** The JWT access token. Client must send this in Authorization header. */
    private String accessToken;

    /** Always "Bearer" — tells the client HOW to use the token. */
    @Builder.Default
    private String tokenType = "Bearer";

    /** The logged-in user's database ID. Useful for frontend to build profile URLs. */
    private Long userId;

    private String email;
    private String name;

    /** e.g., "ROLE_USER" or "ROLE_ADMIN" — client uses this for UI permissions */
    private String role;

    /**
     * Token expiry in milliseconds from issuance.
     * 86400000 ms = 24 hours.
     * Client can use this to show "session expires in X hours" or auto-refresh.
     */
    @Builder.Default
    private Long expiresIn = 86400000L;
}
