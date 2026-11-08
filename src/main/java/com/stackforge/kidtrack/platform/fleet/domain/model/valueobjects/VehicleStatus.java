package com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects;

import java.util.Locale;

/** Availability of a vehicle. */
public enum VehicleStatus {
    ACTIVE,
    INACTIVE;

    public static VehicleStatus from(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid vehicle status.");
        }
    }
}
