package com.stackforge.kidtrack.platform.iam.domain.model.valueobjects;

import java.util.Locale;

/** Role of a user inside the platform. */
public enum RoleTier {
    ADMIN,
    DRIVER,
    PARENT;

    /** Reads a role name case-insensitively. */
    public static RoleTier from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The role tier is required.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid role tier.");
        }
    }
}
