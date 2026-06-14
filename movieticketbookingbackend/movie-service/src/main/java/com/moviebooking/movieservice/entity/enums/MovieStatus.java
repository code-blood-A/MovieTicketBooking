package com.moviebooking.movieservice.entity.enums;

/**
 * Lifecycle status of a movie in the system.
 *
 * WHY track status?
 * BookMyShow shows "Now Showing" and "Coming Soon" tabs.
 * Status lets the Show Service filter: only NOW_SHOWING movies can have active shows.
 * ENDED movies are archived — no new shows, but booking history is preserved.
 */
public enum MovieStatus {

    /** Currently playing in theatres — shows can be created for this movie */
    NOW_SHOWING,

    /** Announced but not yet released — no shows yet, users can set reminders */
    COMING_SOON,

    /** Theatrical run is over — no new shows, historical data preserved */
    ENDED
}
