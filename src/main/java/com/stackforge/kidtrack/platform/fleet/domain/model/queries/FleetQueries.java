package com.stackforge.kidtrack.platform.fleet.domain.model.queries;

import java.util.UUID;

/** Queries handled by the Fleet bounded context. */
public final class FleetQueries {

    private FleetQueries() {
    }

    public record GetVehiclesQuery(UUID organizationId) {
    }

    public record GetVehicleByIdQuery(UUID vehicleId) {
    }

    public record GetRoutesQuery(UUID organizationId) {
    }

    public record GetRouteByIdQuery(UUID routeId) {
    }
}
