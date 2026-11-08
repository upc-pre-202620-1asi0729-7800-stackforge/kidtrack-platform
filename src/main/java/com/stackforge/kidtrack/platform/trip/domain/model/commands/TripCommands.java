package com.stackforge.kidtrack.platform.trip.domain.model.commands;

import java.util.Map;
import java.util.UUID;

/** Commands handled by the Trip bounded context. */
public final class TripCommands {

    private TripCommands() {
    }

    /** Schedules a trip of a route; driver, vehicle and students are taken from the route. */
    public record CreateTripCommand(UUID organizationId, UUID routeId, String scheduledDate,
                                    String scheduledStartTime) {
    }

    /**
     * Partial update of a trip. {@code changes} holds only the fields the caller sent (a field with a
     * {@code null} value is a request to clear it): status, startTime, endTime, studentsBoarded, currentStop,
     * currentLocation.
     */
    public record PatchTripCommand(UUID tripId, Map<String, Object> changes) {
    }

    public record DeleteTripCommand(UUID tripId) {
    }

    public record ReportIncidentCommand(UUID organizationId, UUID tripId, UUID routeId, String type, String severity,
                                        String description, String reportedBy, String timestamp) {
    }

    /** Partial update of an incident: its status. */
    public record PatchIncidentCommand(UUID incidentId, String status) {
    }
}
