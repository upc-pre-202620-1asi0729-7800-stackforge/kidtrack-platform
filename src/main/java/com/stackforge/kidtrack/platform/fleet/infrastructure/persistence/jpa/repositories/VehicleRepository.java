package com.stackforge.kidtrack.platform.fleet.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Vehicle;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findByOrganizationId(UUID organizationId);

    boolean existsByOrganizationIdAndPlate(UUID organizationId, String plate);
}
