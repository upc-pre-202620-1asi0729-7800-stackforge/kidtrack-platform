package com.stackforge.kidtrack.platform.shared.domain.model.valueobjects;

import java.util.UUID;

/** Helpers to read the identifiers sent by the web application, which always arrive as text. */
public final class Ids {

    private Ids() {
    }

    /** Returns {@code null} for a null or blank value and fails with a clear message for a malformed one. */
    public static UUID parseOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("The identifier is not valid: " + value);
        }
    }

    /** Same as {@link #parseOrNull(String)} but the value is mandatory. */
    public static UUID parse(String value, String field) {
        var parsed = parseOrNull(value);
        if (parsed == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return parsed;
    }

    public static String asText(UUID value) {
        return value == null ? null : value.toString();
    }
}
