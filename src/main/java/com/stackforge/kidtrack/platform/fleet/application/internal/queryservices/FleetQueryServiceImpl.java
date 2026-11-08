package com.stackforge.kidtrack.platform.fleet.application.internal.queryservices;

import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Route;
import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Vehicle;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetRouteByIdQuery;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetRoutesQuery;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetVehicleByIdQuery;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetVehiclesQuery;
import com.stackforge.kidtrack.platform.fleet.domain.services.FleetQueryService;
import com.stackforge.kidtrack.platform.fleet.infrastructure.persistence.jpa.repositories.RouteRepository;
import com.stackforge.kidtrack.platform.fleet.infrastructure.persistence.jpa.repositories.VehicleRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FleetQueryServiceImpl implements FleetQueryService {

    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;

    public FleetQueryServiceImpl(VehicleRepository vehicleRepository, RouteRepository routeRepository) {
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
    }

    @Override
    public List<Vehicle> handle(GetVehiclesQuery query) {
        return query.organizationId() == null
                ? vehicleRepository.findAll() : vehicleRepository.findByOrganizationId(query.organizationId());
    }

    @Override
    public Optional<Vehicle> handle(GetVehicleByIdQuery query) {
        return vehicleRepository.findById(query.vehicleId());
    }

    @Override
    public List<Route> handle(GetRoutesQuery query) {
        return query.organizationId() == null
                ? routeRepository.findAll() : routeRepository.findByOrganizationId(query.organizationId());
    }

    @Override
    public Optional<Route> handle(GetRouteByIdQuery query) {
        return routeRepository.findById(query.routeId());
    }
}
