package com.stackforge.kidtrack.platform.fleet.domain.model.commands;

import java.util.List;
import java.util.UUID;

/** Commands handled by the Fleet bounded context. */
public final class FleetCommands {

    private FleetCommands() {
    }

    public record CreateVehicleCommand(UUID organizationId, String plate, String model, int capacity, String status) {
    }

    public record UpdateVehicleCommand(UUID vehicleId, String plate, String model, Integer capacity, String status) {
    }

    public record DeleteVehicleCommand(UUID vehicleId) {
    }

    /** A stop as sent by the web application. */
    public record WaypointData(Integer order, String name, double lat, double lng, List<String> studentIds) {
    }

    public record CreateRouteCommand(UUID organizationId, String name, String type, UUID driverId, String driverName,
                                     UUID vehicleId, String vehiclePlate, List<String> studentIds,
                                     String scheduledStartTime, String status, List<WaypointData> waypoints) {
    }

    /** Full replacement of the configuration of a route. */
    public record UpdateRouteCommand(UUID routeId, String name, String type, UUID driverId, String driverName,
                                     UUID vehicleId, String vehiclePlate, List<String> studentIds,
                                     String scheduledStartTime, String status, List<WaypointData> waypoints) {
    }

    public record DeleteRouteCommand(UUID routeId) {
    }
}
