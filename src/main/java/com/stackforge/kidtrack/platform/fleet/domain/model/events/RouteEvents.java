package com.stackforge.kidtrack.platform.fleet.domain.model.events;

import java.util.List;
import java.util.UUID;

/** Domain events published by the Fleet bounded context for the contexts that depend on it. */
public final class RouteEvents {

    private RouteEvents() {
    }

    /** The configuration of a route changed: the trips that are still pending follow its new driver and vehicle. */
    public record RouteConfigurationChanged(UUID routeId, UUID driverId, String driverName, UUID vehicleId,
                                            String vehiclePlate, List<String> studentIds) {
    }

    /** A route was deleted together with its history. */
    public record RouteDeleted(UUID routeId) {
    }
}
