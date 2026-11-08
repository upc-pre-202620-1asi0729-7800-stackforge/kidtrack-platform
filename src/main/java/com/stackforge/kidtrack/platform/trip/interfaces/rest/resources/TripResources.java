package com.stackforge.kidtrack.platform.trip.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** REST resources of the Trip bounded context. Identifiers travel as text, like in the web application. */
public final class TripResources {

    private TripResources() {
    }

    public record TripResource(String id, String routeId, String routeName, String driverId, String driverName,
                               String vehicleId, String vehiclePlate, List<String> studentIds, String tripType,
                               String scheduledDate, String scheduledStartTime, String status, String startTime,
                               String endTime, int studentsTotal, int studentsBoarded, String currentStop,
                               String currentLocation, String organizationId) {
    }

    /**
     * Schedules a trip of a route. The driver, vehicle and students come from the route; the other fields the web
     * application sends along are accepted and ignored.
     */
    public record TripRequestResource(@NotBlank String routeId, String scheduledDate, String scheduledStartTime,
                                      String organizationId) {
    }

    public record IncidentResource(String id, String tripId, String routeId, String routeName, String type,
                                   String severity, String description, String reportedBy, String timestamp,
                                   String status, String organizationId) {
    }

    public record IncidentRequestResource(String tripId, String routeId, String type, String severity,
                                          @NotBlank String description, String reportedBy, String timestamp,
                                          String organizationId) {
    }

    public record IncidentPatchResource(String status) {
    }
}
