package com.moviebooking.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RegisterRequest DTO — Data Transfer Object for user registration.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY DTO instead of using the User entity directly?
 * ═══════════════════════════════════════════════════════════
 * Two critical reasons:
 *
 * 1. SECURITY: If you accept a User entity in the controller, a malicious
 *    client could send { "role": "ROLE_ADMIN" } and make themselves admin.
 *    With DTO, only the fields you EXPOSE in the DTO can be set.
 *    The 'role' field is NOT in RegisterRequest → clients can never set it.
 *
 * 2. FLEXIBILITY: Your API contract (RegisterRequest) is separate from your
 *    DB schema (User entity). You can:
 *    - Add a 'confirmPassword' field to the DTO without adding it to the DB
 *    - Rename a DB column without changing the API
 *    - Accept 'fullName' in the API but store as 'name' in DB
 *
 * ═══════════════════════════════════════════════════════════
 * BEAN VALIDATION ANNOTATIONS
 * ═══════════════════════════════════════════════════════════
 * These annotations are DECLARATIONS of constraints. They do nothing alone.
 * They activate when the controller uses @Valid on the parameter:
 *   public ResponseEntity<> register(@Valid @RequestBody RegisterRequest req)
 *
 * On invalid input → MethodArgumentNotValidException
 * → Caught by GlobalExceptionHandler → returns 400 Bad Request with details
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    /**
     * Password validation:
     * - @NotBlank: not null, not empty, not just whitespace
     * - @Size: minimum 8 characters
     * - @Pattern: must contain at least one letter and one digit
     *   (real apps add more requirements, kept simple for learning)
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "Password must contain at least one letter and one number"
    )
    private String password;

    /** Optional — no validation constraint */
    @Pattern(
        regexp = "^[+]?[0-9]{10,15}$",
        message = "Please provide a valid phone number"
    )
    private String phone;
}
