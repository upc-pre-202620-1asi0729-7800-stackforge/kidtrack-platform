package com.stackforge.kidtrack.platform.fleet.interfaces.rest.transform;

import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Route;
import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Vehicle;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.WaypointData;
import com.stackforge.kidtrack.platform.fleet.domain.model.entities.Stop;
import com.stackforge.kidtrack.platform.fleet.interfaces.rest.resources.FleetResources.RouteResource;
import com.stackforge.kidtrack.platform.fleet.interfaces.rest.resources.FleetResources.VehicleResource;
import com.stackforge.kidtrack.platform.fleet.interfaces.rest.resources.FleetResources.WaypointResource;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import java.util.List;

/** Converts fleet aggregates into REST resources and back. */
public final class FleetResourceAssembler {

    private FleetResourceAssembler() {
    }

    public static VehicleResource toResource(Vehicle vehicle) {
        return new VehicleResource(vehicle.getId().toString(), vehicle.getPlate(), vehicle.getModel(),
                vehicle.getCapacity(), vehicle.getStatus().name(), vehicle.getOrganizationId().toString());
    }

    public static RouteResource toResource(Route route) {
        return new RouteResource(
                route.getId().toString(),
                route.getName(),
                route.getType().name(),
                Ids.asText(route.getDriverId()),
                route.getDriverName(),
                Ids.asText(route.getVehicleId()),
                route.getVehiclePlate(),
                List.copyOf(route.getStudentIds()),
                route.getScheduledStartTime(),
                route.getStatus().name(),
                route.getOrganizationId().toString(),
                route.getStops().stream().map(FleetResourceAssembler::toResource).toList());
    }

    private static WaypointResource toResource(Stop stop) {
        return new WaypointResource(stop.getPosition(), stop.getName(), stop.getCoordinates().latitude(),
                stop.getCoordinates().longitude(), List.copyOf(stop.getStudentIds()));
    }

    public static List<WaypointData> toWaypointData(List<WaypointResource> waypoints) {
        if (waypoints == null) {
            return List.of();
        }
        return waypoints.stream()
                .map(waypoint -> new WaypointData(waypoint.order(), waypoint.name(), waypoint.lat(), waypoint.lng(),
                        waypoint.studentIds()))
                .toList();
    }
}
