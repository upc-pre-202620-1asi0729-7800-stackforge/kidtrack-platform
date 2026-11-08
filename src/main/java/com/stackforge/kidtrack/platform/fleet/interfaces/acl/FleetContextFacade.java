package com.stackforge.kidtrack.platform.fleet.interfaces.acl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Anti-corruption facade that exposes the route data other bounded contexts need. */
public interface FleetContextFacade {

    /** Read-only view of a route. */
    record RouteSummary(UUID id, UUID organizationId, String name, String type, String status, UUID driverId,
                        String driverName, UUID vehicleId, String vehiclePlate, List<String> studentIds,
                        String scheduledStartTime, List<String> stopNames, boolean complete) {
    }

    Optional<RouteSummary> findRoute(UUID routeId);
}
