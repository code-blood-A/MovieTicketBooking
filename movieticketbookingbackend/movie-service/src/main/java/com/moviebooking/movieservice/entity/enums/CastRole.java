package com.moviebooking.movieservice.entity.enums;

/**
 * Role of a person in a movie's cast/crew.
 *
 * This enum is used in the Cast entity to identify whether a person
 * is an actor, director, or producer for a particular movie.
 * The same person (e.g., "Aamir Khan") can be ACTOR in one movie
 * and DIRECTOR+ACTOR in another — each is a separate Cast record.
 */
public enum CastRole {
    ACTOR,
    DIRECTOR,
    PRODUCER
}
