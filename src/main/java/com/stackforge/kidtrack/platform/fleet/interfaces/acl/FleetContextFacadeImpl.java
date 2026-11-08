package com.stackforge.kidtrack.platform.fleet.interfaces.acl;

import com.stackforge.kidtrack.platform.fleet.domain.model.entities.Stop;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetRouteByIdQuery;
import com.stackforge.kidtrack.platform.fleet.domain.services.FleetQueryService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FleetContextFacadeImpl implements FleetContextFacade {

    private final FleetQueryService queryService;

    public FleetContextFacadeImpl(FleetQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public Optional<RouteSummary> findRoute(UUID routeId) {
        if (routeId == null) {
            return Optional.empty();
        }
        return queryService.handle(new GetRouteByIdQuery(routeId)).map(route -> new RouteSummary(
                route.getId(), route.getOrganizationId(), route.getName(), route.getType().name(),
                route.getStatus().name(), route.getDriverId(), route.getDriverName(), route.getVehicleId(),
                route.getVehiclePlate(), List.copyOf(route.getStudentIds()), route.getScheduledStartTime(),
                route.getStops().stream().map(Stop::getName).toList(), route.isComplete()));
    }
}
