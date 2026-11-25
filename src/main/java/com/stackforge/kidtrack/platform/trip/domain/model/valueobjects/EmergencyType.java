package com.stackforge.kidtrack.platform.trip.domain.model.valueobjects;

import java.util.Locale;

/** Force majeure situations that require an external service, and the Peruvian number that attends each one. */
public enum EmergencyType {
    ACCIDENTE("accidente de tránsito", "la Policía", "105"),
    ASALTO("asalto", "la Policía", "105"),
    MEDICA("emergencia médica", "el SAMU", "106"),
    INCENDIO("incendio o personas atrapadas", "los Bomberos", "116");

    private final String label;
    private final String serviceName;
    private final String servicePhone;

    EmergencyType(String label, String serviceName, String servicePhone) {
        this.label = label;
        this.serviceName = serviceName;
        this.servicePhone = servicePhone;
    }

    /** Returns {@code null} when no type is given: the incident is then an ordinary one. */
    public static EmergencyType fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid emergency type.");
        }
    }

    public String label() {
        return label;
    }

    public String serviceName() {
        return serviceName;
    }

    public String servicePhone() {
        return servicePhone;
    }
}
