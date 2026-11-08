package com.stackforge.kidtrack.platform.shared.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Shared value object for the full name of a person. The last name may be empty because
 * the web application captures a single "name" field for some stakeholders.
 */
@Embeddable
public record FullName(
        @Column(name = "first_name", length = 100) String firstName,
        @Column(name = "last_name", length = 100) String lastName) {

    public static final int MAX_LENGTH = 100;

    public FullName {
        firstName = firstName == null ? "" : firstName.trim();
        lastName = lastName == null ? "" : lastName.trim();
        if (firstName.isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty.");
        }
        if (firstName.length() > MAX_LENGTH || lastName.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Name values are too long.");
        }
    }

    /** Splits a free text name into first name (first word) and last name (the rest). */
    public static FullName fromText(String text) {
        var value = text == null ? "" : text.trim();
        var separator = value.indexOf(' ');
        if (separator < 0) {
            return new FullName(value, "");
        }
        return new FullName(value.substring(0, separator), value.substring(separator + 1));
    }

    @Override
    public String toString() {
        return (firstName + " " + lastName).trim();
    }
}
