package com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects;

import java.util.Locale;

/** Lifecycle state of a subscription. */
public enum SubscriptionState {
    ACTIVE,
    CANCELLED,
    EXPIRED;

    public static SubscriptionState from(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid subscription state.");
        }
    }
}
