package com.stackforge.kidtrack.platform.trip.domain.model.valueobjects;

import java.util.Locale;

/** Lifecycle status of a trip. */
public enum TripStatus {
    SCHEDULED,
    EN_ROUTE,
    COMPLETED,
    CANCELLED;

    public static TripStatus from(String value) {
        if (value == null || value.isBlank()) {
            return SCHEDULED;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid trip status.");
        }
    }
}
