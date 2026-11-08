package com.stackforge.kidtrack.platform.notifications.domain.model.valueobjects;

import java.util.Locale;

/** Kind of notice a parent receives about a trip of their children. */
public enum NotificationType {
    ABORDAJE,
    PROXIMIDAD,
    AUSENCIA,
    LLEGADA,
    RETRASO;

    public static NotificationType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The notification type is required.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid notification type.");
        }
    }
}
