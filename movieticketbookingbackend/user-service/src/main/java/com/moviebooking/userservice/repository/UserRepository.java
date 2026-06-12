package com.moviebooking.userservice.repository;

import com.moviebooking.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository — Data Access Layer for the User entity.
 *
 * ═══════════════════════════════════════════════════════════
 * HOW Spring Data JPA WORKS (the magic behind the interface)
 * ═══════════════════════════════════════════════════════════
 * At startup, Spring scans for interfaces extending JpaRepository.
 * It generates a PROXY class at runtime that implements all the methods.
 * You write an interface → Spring writes the implementation for you.
 *
 * JpaRepository<User, Long> gives us for FREE:
 *   save(user)          → INSERT or UPDATE
 *   findById(id)        → SELECT WHERE id = ?
 *   findAll()           → SELECT * FROM users
 *   deleteById(id)      → DELETE WHERE id = ?
 *   existsById(id)      → SELECT COUNT(*) WHERE id = ?
 *   count()             → SELECT COUNT(*) FROM users
 *   ... and more
 *
 * ═══════════════════════════════════════════════════════════
 * DERIVED QUERY METHODS (Spring Data magic)
 * ═══════════════════════════════════════════════════════════
 * Method names are PARSED by Spring to generate SQL automatically:
 *
 * findByEmail(email)
 *   → SELECT * FROM users WHERE email = ?
 *
 * existsByEmail(email)
 *   → SELECT COUNT(*) > 0 FROM users WHERE email = ?
 *
 * The naming convention:
 *   findBy + [FieldName] + [Operator]
 *   find    = SELECT
 *   By      = WHERE
 *   Email   = email column
 *   And/Or  = AND/OR conditions
 *   Like    = LIKE ?
 *   Between = BETWEEN ? AND ?
 *
 * ═══════════════════════════════════════════════════════════
 * WHY Optional<User> instead of User?
 * ═══════════════════════════════════════════════════════════
 * findByEmail returns Optional because the user might NOT exist.
 * Optional forces the caller to handle both cases explicitly:
 *   userRepository.findByEmail(email)
 *     .orElseThrow(() -> new UserNotFoundException("..."))
 *
 * Returning null would allow NullPointerException bugs to slip through.
 * Optional makes "user might not exist" part of the method's type contract.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     * Used during LOGIN to look up the user before verifying the password.
     *
     * Generated SQL: SELECT * FROM users WHERE email = ? LIMIT 1
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with this email already exists.
     * Used during REGISTRATION to prevent duplicate accounts.
     *
     * Generated SQL: SELECT count(*) FROM users WHERE email = ?
     * Returns true if count > 0.
     *
     * WHY existsByEmail instead of findByEmail + isPresent()?
     * existsByEmail is MORE EFFICIENT — it only does a COUNT query
     * (no need to load the full User object from DB just to check existence).
     */
    boolean existsByEmail(String email);
}
