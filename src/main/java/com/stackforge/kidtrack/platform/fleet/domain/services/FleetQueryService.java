package com.stackforge.kidtrack.platform.fleet.domain.services;

import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Route;
import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Vehicle;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetRouteByIdQuery;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetRoutesQuery;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetVehicleByIdQuery;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetVehiclesQuery;
import java.util.List;
import java.util.Optional;

/** Read side of the Fleet bounded context. */
public interface FleetQueryService {

    List<Vehicle> handle(GetVehiclesQuery query);

    Optional<Vehicle> handle(GetVehicleByIdQuery query);

    List<Route> handle(GetRoutesQuery query);

    Optional<Route> handle(GetRouteByIdQuery query);
}
