package com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects;

import java.util.Locale;

/** Lifecycle status of a route. */
public enum RouteStatus {
    DRAFT,
    ACTIVE,
    INACTIVE;

    public static RouteStatus from(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid route status.");
        }
    }
}
