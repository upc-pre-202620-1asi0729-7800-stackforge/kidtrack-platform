package com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects;

import java.util.Locale;

/** Direction of a route: OUTBOUND picks the students up (recojo) and RETURN drops them off (retorno). */
public enum RouteType {
    OUTBOUND,
    RETURN;

    public static RouteType from(String value) {
        if (value == null || value.isBlank()) {
            return OUTBOUND;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'" + value + "' is not a valid route type.");
        }
    }
}
