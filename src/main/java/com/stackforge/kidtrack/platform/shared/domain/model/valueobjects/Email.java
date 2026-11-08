package com.stackforge.kidtrack.platform.shared.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.regex.Pattern;

/** Shared value object for an e-mail address. It is always stored in lower case. */
@Embeddable
public record Email(@Column(name = "email", length = 255) String value) {

    public static final int MAX_LENGTH = 255;
    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH || !FORMAT.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
