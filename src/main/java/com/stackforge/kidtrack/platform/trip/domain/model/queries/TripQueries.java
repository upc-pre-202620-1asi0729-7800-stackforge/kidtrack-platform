package com.stackforge.kidtrack.platform.trip.domain.model.queries;

import java.util.UUID;

/** Queries handled by the Trip bounded context. */
public final class TripQueries {

    private TripQueries() {
    }

    /** Lists the trips of an organization; every other criterion is optional. */
    public record GetTripsQuery(UUID organizationId, UUID routeId, UUID driverId, String status,
                                String scheduledDate) {
    }

    public record GetTripByIdQuery(UUID tripId) {
    }

    /** Lists the incidents of an organization; every other criterion is optional. */
    public record GetIncidentsQuery(UUID organizationId, UUID tripId, String status) {
    }

    public record GetIncidentByIdQuery(UUID incidentId) {
    }
}
