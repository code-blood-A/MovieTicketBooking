package com.moviebooking.userservice.service;

import com.moviebooking.userservice.dto.AuthResponse;
import com.moviebooking.userservice.dto.LoginRequest;
import com.moviebooking.userservice.dto.RegisterRequest;
import com.moviebooking.userservice.dto.UserProfileResponse;

/**
 * UserService interface — the contract for all user business operations.
 *
 * ═══════════════════════════════════════════════════════════
 * LLD PATTERN: Programming to an Interface (not an Implementation)
 * ═══════════════════════════════════════════════════════════
 * WHY define an interface separately instead of just writing the class?
 *
 * 1. DECOUPLING: The Controller depends on UserService (interface), not on
 *    UserServiceImpl (the concrete class). This means:
 *    - You can swap the implementation without touching the Controller
 *    - You can have multiple implementations (e.g., UserServiceCachedImpl)
 *
 * 2. TESTABILITY: In unit tests, you can MOCK this interface:
 *    @Mock UserService userService;
 *    when(userService.login(request)).thenReturn(mockResponse);
 *    → Test the Controller without a real database.
 *    Without an interface, mocking is harder and more brittle.
 *
 * 3. INVERSION OF CONTROL: Controller says "I need SOMETHING that can register
 *    and login users" (depends on abstraction). Spring injects the implementation.
 *    This is the Dependency Inversion Principle (D in SOLID).
 *
 * 4. DOCUMENTATION: The interface serves as a clear API contract — what operations
 *    does this service expose? What inputs/outputs? All visible at a glance
 *    without reading 200 lines of implementation.
 *
 * ═══════════════════════════════════════════════════════════
 * When is an interface overkill?
 * ═══════════════════════════════════════════════════════════
 * For very simple CRUD operations with no complex logic and only one
 * implementation ever, some teams skip the interface. But for a learning
 * project, always write the interface — it trains the right habits.
 */
public interface UserService {

    /**
     * Register a new user account.
     *
     * @param request validated registration data (name, email, password, phone)
     * @return AuthResponse containing JWT token + user info (user is auto-logged in after registration)
     * @throws com.moviebooking.userservice.exception.UserAlreadyExistsException if email is taken
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate a user with email + password.
     *
     * @param request login credentials
     * @return AuthResponse containing JWT token + user info
     * @throws com.moviebooking.userservice.exception.UserNotFoundException if credentials are wrong
     */
    AuthResponse login(LoginRequest request);

    /**
     * Fetch the profile of a specific user.
     * Called with userId extracted from the JWT by the Gateway (X-User-Id header).
     *
     * @param userId the user's database ID (from X-User-Id header set by Gateway)
     * @return safe public profile (no password)
     * @throws com.moviebooking.userservice.exception.UserNotFoundException if user doesn't exist
     */
    UserProfileResponse getUserProfile(Long userId);
}
