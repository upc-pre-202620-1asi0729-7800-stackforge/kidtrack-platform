package com.stackforge.kidtrack.platform.iam.domain.model.valueobjects;

import java.util.Locale;

/** Lifecycle status of an organization. */
public enum OrganizationStatus {
    ACTIVE,
    SUSPENDED;

    /** Reads a status case-insensitively. */
    public static OrganizationStatus from(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid organization status.");
        }
    }
}
