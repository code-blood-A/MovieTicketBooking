package com.moviebooking.theatreservice.entity.enums;

/**
 * The screen/auditorium technology type.
 * Affects the ticket price multiplier and show-booking eligibility.
 *
 * TWO_D   → Standard 2D (most common, cheapest)
 * THREE_D → 3D projection (glasses required, higher price)
 * IMAX    → Large format, immersive (2x price premium)
 * FOUR_DX → Moving seats + effects (3x price premium)
 */
public enum ScreenType {
    TWO_D,
    THREE_D,
    IMAX,
    FOUR_DX
}
