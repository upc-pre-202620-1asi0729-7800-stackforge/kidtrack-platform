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

    public record IncidentUpdateResource(String message, String sentAt, String author) {
    }

    /** The fields from {@code emergencyType} on are only filled for force majeure emergencies. */
    public record IncidentResource(String id, String tripId, String routeId, String routeName, String type,
                                   String severity, String description, String reportedBy, String timestamp,
                                   String status, String organizationId, String emergencyType, String serviceName,
                                   String servicePhone, String location, List<String> studentIds,
                                   Boolean studentsSafe, String serviceContactedAt, String serviceContactedBy,
                                   String resolution, String resolvedAt, List<IncidentUpdateResource> updates) {
    }

    /** Send an {@code emergencyType} (ACCIDENTE, ASALTO, MEDICA or INCENDIO) to declare an emergency. */
    public record IncidentRequestResource(String tripId, String routeId, String type, String severity,
                                          @NotBlank String description, String reportedBy, String timestamp,
                                          String organizationId, String emergencyType, String location,
                                          Boolean studentsSafe, List<String> studentIds) {
    }

    public record IncidentPatchResource(String status, String resolution) {
    }

    public record IncidentUpdateRequestResource(@NotBlank String message, Boolean studentsSafe) {
    }

    public record HandoverResource(String id, String tripId, String childId, String childName,
                                   String authorizedPersonId, String receiverName, String relationship,
                                   String outcome, String recordedBy, String recordedAt, String organizationId) {
    }

    /** Leave {@code authorizedPersonId} empty when the person at the stop is not registered (not delivered). */
    public record HandoverRequestResource(@NotBlank String tripId, @NotBlank String childId,
                                          String authorizedPersonId, String receiverName) {
    }
}
