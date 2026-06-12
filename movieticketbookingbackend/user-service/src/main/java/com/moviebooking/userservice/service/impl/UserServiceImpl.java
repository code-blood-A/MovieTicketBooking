package com.moviebooking.userservice.service.impl;

import com.moviebooking.userservice.dto.AuthResponse;
import com.moviebooking.userservice.dto.LoginRequest;
import com.moviebooking.userservice.dto.RegisterRequest;
import com.moviebooking.userservice.dto.UserProfileResponse;
import com.moviebooking.userservice.entity.User;
import com.moviebooking.userservice.entity.enums.Role;
import com.moviebooking.userservice.exception.UserAlreadyExistsException;
import com.moviebooking.userservice.exception.UserNotFoundException;
import com.moviebooking.userservice.repository.UserRepository;
import com.moviebooking.userservice.security.JwtService;
import com.moviebooking.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserServiceImpl — Business logic implementation.
 *
 * ═══════════════════════════════════════════════════════════
 * @Service vs @Component
 * ═══════════════════════════════════════════════════════════
 * Both register a bean in the Spring container, but:
 * @Service is SEMANTICALLY meaningful — it tells developers "this class
 * contains business logic." Same bytecode, different intent communication.
 * Use @Service for business logic, @Repository for data access, @Component
 * for generic Spring-managed beans.
 *
 * ═══════════════════════════════════════════════════════════
 * @RequiredArgsConstructor (Constructor Injection)
 * ═══════════════════════════════════════════════════════════
 * Generates a constructor for all 'final' fields.
 * Spring sees this constructor and injects the beans automatically.
 *
 * WHY constructor injection over @Autowired on fields?
 * 1. final fields → immutable after construction (thread safe)
 * 2. Explicit dependencies → visible in constructor signature
 * 3. Easy to test → just call new UserServiceImpl(mockRepo, mockJwt, mockEncoder)
 * 4. No Spring needed in unit tests (unlike field injection)
 *
 * ═══════════════════════════════════════════════════════════
 * @Transactional
 * ═══════════════════════════════════════════════════════════
 * Methods annotated with @Transactional run inside a database transaction.
 * If the method throws a RuntimeException → transaction is ROLLED BACK.
 * If the method completes normally → transaction is COMMITTED.
 *
 * WHY on register() but not on login()?
 * register() WRITES to the DB (save user) — needs a transaction.
 * login() only READS — @Transactional(readOnly=true) would work but
 * for simplicity we skip it here.
 *
 * WHY NOT put @Transactional on the interface?
 * It's a Spring proxy mechanism detail — putting it on the implementation
 * class (or its methods) is the correct place.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;  // Bean defined in SecurityConfig

    /**
     * Register a new user.
     *
     * FLOW:
     * 1. Check email uniqueness (fast DB query)
     * 2. Hash the password with BCrypt
     * 3. Build and save the User entity
     * 4. Generate JWT for the new user
     * 5. Return AuthResponse (user is logged in immediately after registration)
     *
     * WHY return a JWT after registration?
     * UX: Users hate registering and then having to log in again. Most modern
     * apps (Swiggy, BookMyShow) log you in immediately after signup.
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        // STEP 1: Guard clause — fail fast if email is taken
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException(
                "An account with email '" + request.getEmail() + "' already exists"
            );
        }

        // STEP 2: Build the User entity.
        // Note: role defaults to ROLE_USER (set in the entity with @Builder.Default)
        // Users can never self-promote to ROLE_ADMIN — that's only done by an admin.
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash
                .phone(request.getPhone())
                .role(Role.ROLE_USER) // Always ROLE_USER on self-registration
                .build();

        // STEP 3: Persist to DB — Hibernate generates: INSERT INTO users (...)
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        // STEP 4: Generate JWT
        String token = jwtService.generateToken(savedUser);

        // STEP 5: Build and return the response using Builder Pattern
        return AuthResponse.builder()
                .accessToken(token)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
                .build();
    }

    /**
     * Authenticate a user with email + password.
     *
     * SECURITY NOTE: We deliberately use the SAME error message for both
     * "email not found" and "wrong password" scenarios: "Invalid email or password".
     *
     * WHY? To prevent USER ENUMERATION attacks:
     * If we returned "email not found," an attacker could test emails in bulk
     * to discover which ones have accounts (then target those for phishing).
     * Generic error message = attacker learns nothing useful.
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // STEP 1: Fetch user by email
        // orElseThrow = if not found, throw exception immediately (same generic message)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — email not found: {}", request.getEmail());
                    // Generic message to prevent user enumeration
                    return new UserNotFoundException("Invalid email or password");
                });

        // STEP 2: Verify password
        // passwordEncoder.matches(rawPassword, storedHash) — BCrypt handles this
        // BCrypt re-hashes rawPassword with the salt embedded in storedHash, then compares
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed — wrong password for email: {}", request.getEmail());
            throw new UserNotFoundException("Invalid email or password"); // Same generic message
        }

        // STEP 3: Generate fresh JWT
        String token = jwtService.generateToken(user);
        log.info("Login successful for userId: {}", user.getId());

        return AuthResponse.builder()
                .accessToken(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Fetch a user's profile by ID.
     *
     * The userId comes from the X-User-Id header set by the API Gateway
     * after validating the JWT. The controller extracts it and passes it here.
     *
     * @Transactional(readOnly=true) optimization:
     * Tells Hibernate this transaction won't modify data →
     * - Skips dirty checking (no need to compare entity state with snapshot)
     * - Database driver can use read replicas if configured
     * Small optimization, good habit for read-only methods.
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        log.debug("Fetching profile for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Convert entity → DTO (never expose the entity directly)
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
