package com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Incident;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByOrganizationId(UUID organizationId);

    List<Incident> findByRouteId(UUID routeId);

    List<Incident> findByTripId(UUID tripId);
}
