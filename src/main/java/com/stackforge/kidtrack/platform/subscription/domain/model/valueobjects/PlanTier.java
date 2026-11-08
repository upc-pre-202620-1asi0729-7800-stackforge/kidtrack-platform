package com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects;

import java.util.Locale;

/** Commercial tier of a plan. */
public enum PlanTier {
    BASIC,
    INTERMEDIATE,
    COMPLETE;

    public static PlanTier from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The plan tier is required.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid plan tier.");
        }
    }
}
