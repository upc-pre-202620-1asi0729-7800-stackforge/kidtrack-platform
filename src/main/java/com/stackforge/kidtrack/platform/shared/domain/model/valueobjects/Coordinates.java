package com.stackforge.kidtrack.platform.shared.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Shared value object representing a WGS-84 geographic location. */
@Embeddable
public record Coordinates(
        @Column(name = "latitude") double latitude,
        @Column(name = "longitude") double longitude) {

    public Coordinates {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }
    }
}
