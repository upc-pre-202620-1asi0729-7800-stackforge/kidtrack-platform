package com.stackforge.kidtrack.platform.trip.domain.model.valueobjects;

import java.util.Locale;

/** Resolution status of an incident. */
public enum IncidentStatus {
    OPEN,
    RESOLVED;

    public static IncidentStatus from(String value) {
        if (value == null || value.isBlank()) {
            return OPEN;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid incident status.");
        }
    }
}
