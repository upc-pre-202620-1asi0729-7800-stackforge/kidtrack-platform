package com.stackforge.kidtrack.platform.stakeholder.domain.model.valueobjects;

import java.util.Locale;

/** Latest boarding status of a student, as recorded by the driver during a trip. */
public enum BoardingStatus {
    EN_ESPERA,
    ABORDADO,
    AUSENTE;

    public static BoardingStatus from(String value) {
        if (value == null || value.isBlank()) {
            return EN_ESPERA;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid boarding status.");
        }
    }
}
