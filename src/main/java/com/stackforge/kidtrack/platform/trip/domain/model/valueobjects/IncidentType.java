package com.stackforge.kidtrack.platform.trip.domain.model.valueobjects;

import java.util.Locale;

/** Kind of incident a driver can report during a trip. */
public enum IncidentType {
    RETRASO,
    AVERIA,
    ACCIDENTE,
    COMPORTAMIENTO,
    EMERGENCIA,
    OTRO;

    public static IncidentType from(String value) {
        if (value == null || value.isBlank()) {
            return OTRO;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid incident type.");
        }
    }
}
