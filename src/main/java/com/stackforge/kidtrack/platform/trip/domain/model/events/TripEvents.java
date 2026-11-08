package com.stackforge.kidtrack.platform.trip.domain.model.events;

import java.util.UUID;

/** Domain events published by the Trip bounded context. */
public final class TripEvents {

    private TripEvents() {
    }

    /** A trip was deleted; the notifications that refer to it are no longer meaningful. */
    public record TripDeleted(UUID tripId) {
    }
}
