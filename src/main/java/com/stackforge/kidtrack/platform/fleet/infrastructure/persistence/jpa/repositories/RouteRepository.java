package com.stackforge.kidtrack.platform.fleet.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Route;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

    List<Route> findByOrganizationId(UUID organizationId);

    List<Route> findByVehicleId(UUID vehicleId);

    long countByOrganizationId(UUID organizationId);
}
