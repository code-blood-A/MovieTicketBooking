package com.moviebooking.userservice.entity.enums;

/**
 * User roles for authorization.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY prefix with ROLE_?
 * ═══════════════════════════════════════════════════════════
 * Spring Security convention: roles are stored with "ROLE_" prefix.
 * When you call hasRole("USER"), Spring internally checks for "ROLE_USER".
 * By naming our enum values with the prefix, we stay compatible with
 * Spring Security's role-checking mechanisms if we ever use them directly.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY an enum instead of a String in the User entity?
 * ═══════════════════════════════════════════════════════════
 * 1. Type safety — impossible to assign an invalid role like "ROLE_SUPERMAN"
 * 2. Autocomplete in IDE — developers can't typo the role name
 * 3. Refactoring safety — rename in one place, compiler finds all usages
 * 4. In DB: stored as VARCHAR("ROLE_USER") via @Enumerated(EnumType.STRING)
 *    EnumType.STRING is always preferred over EnumType.ORDINAL because:
 *    - ORDINAL stores 0,1,2... If you ADD a role between existing ones, all
 *      ordinals shift and corrupt existing data.
 *    - STRING stores the name — safe to reorder, add, remove enum values.
 */
public enum Role {

    /**
     * Standard registered user.
     * Can: browse movies/shows, book tickets, view own bookings.
     * Cannot: add movies, manage theatres, view all users.
     */
    ROLE_USER,

    /**
     * Administrator.
     * Can: everything ROLE_USER can + manage movies, theatres, shows.
     * In a real system, you'd add ROLE_THEATRE_ADMIN, ROLE_CONTENT_MANAGER, etc.
     */
    ROLE_ADMIN
}
