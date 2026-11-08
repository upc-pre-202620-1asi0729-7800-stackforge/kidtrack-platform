package com.stackforge.kidtrack.platform.trip.domain.model.valueobjects;

import java.util.Locale;

/** Gravity of an incident. */
public enum IncidentSeverity {
    LOW,
    MEDIUM,
    HIGH;

    public static IncidentSeverity from(String value) {
        if (value == null || value.isBlank()) {
            return LOW;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid incident severity.");
        }
    }
}
