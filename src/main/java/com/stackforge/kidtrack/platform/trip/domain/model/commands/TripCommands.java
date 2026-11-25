package com.stackforge.kidtrack.platform.trip.domain.model.commands;

import java.util.List;
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

    /** An {@code emergencyType} turns the report into a force majeure emergency of the trip. */
    public record ReportIncidentCommand(UUID organizationId, UUID tripId, UUID routeId, String type, String severity,
                                        String description, String reportedBy, String timestamp,
                                        String emergencyType, String location, Boolean studentsSafe,
                                        List<String> studentIds) {
    }

    /** Partial update of an incident: its status and how it ended. */
    public record PatchIncidentCommand(UUID incidentId, String status, String resolution) {
    }

    public record RecordServiceCallCommand(UUID incidentId, String contactedBy) {
    }

    public record AddIncidentUpdateCommand(UUID incidentId, String message, Boolean studentsSafe, String author) {
    }

    /** A {@code null} {@code authorizedPersonId} means that nobody registered was there to receive the student. */
    public record RecordHandoverCommand(UUID tripId, UUID childId, UUID authorizedPersonId, String receiverName,
                                        String recordedBy) {
    }
}
