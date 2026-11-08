package com.stackforge.kidtrack.platform.fleet.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** REST resources of the Fleet bounded context. Identifiers travel as text, like in the web application. */
public final class FleetResources {

    private FleetResources() {
    }

    public record WaypointResource(Integer order, String name, double lat, double lng, List<String> studentIds) {
    }

    public record RouteResource(String id, String name, String type, String driverId, String driverName,
                                String vehicleId, String vehiclePlate, List<String> studentIds,
                                String scheduledStartTime, String status, String organizationId,
                                List<WaypointResource> waypoints) {
    }

    public record RouteRequestResource(@NotBlank String name, String type, String driverId, String driverName,
                                       String vehicleId, String vehiclePlate, List<String> studentIds,
                                       String scheduledStartTime, String status, String organizationId,
                                       List<WaypointResource> waypoints) {
    }

    public record VehicleResource(String id, String plate, String model, int capacity, String status,
                                  String organizationId) {
    }

    public record VehicleRequestResource(@NotBlank String plate, @NotBlank String model, int capacity,
                                         String status, String organizationId) {
    }
}
