package com.stackforge.kidtrack.platform.shared.domain.model.valueobjects;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/** Helpers to read the flexible date formats sent by the web application. */
public final class Dates {

    private Dates() {
    }

    /** Parses an ISO instant, an ISO offset date time or a plain ISO date (interpreted at UTC midnight). */
    public static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var text = value.trim();
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            // try the next format
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException ignored) {
            // try the next format
        }
        try {
            return LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("The value is not a valid ISO date: " + value);
        }
    }

    public static String format(Instant value) {
        return value == null ? null : value.toString();
    }
}
