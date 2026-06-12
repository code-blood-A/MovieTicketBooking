package com.moviebooking.userservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * UserProfileResponse — safe, public view of a User.
 *
 * Notice what's NOT here compared to the User entity:
 *   ❌ password  — never expose hashed password in API responses
 *   ❌ updatedAt — internal audit field, not relevant to API consumers
 *
 * This is the "projection" principle: expose only what the consumer needs.
 * A mobile app showing a user profile needs name/email/phone — not internals.
 */
@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private LocalDateTime createdAt;

    /** "Member since" — useful for profile pages */
}
