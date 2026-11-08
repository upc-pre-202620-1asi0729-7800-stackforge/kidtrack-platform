package com.stackforge.kidtrack.platform.trip.domain.model.valueobjects;

import java.util.Locale;

/** Direction of a trip: OUTBOUND picks the students up and RETURN drops them off. */
public enum TripType {
    OUTBOUND,
    RETURN;

    public static TripType from(String value) {
        if (value == null || value.isBlank()) {
            return OUTBOUND;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid trip type.");
        }
    }
}
