package com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Driver;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

    List<Driver> findByOrganizationId(UUID organizationId);

    Optional<Driver> findByUserId(UUID userId);

    long countByOrganizationId(UUID organizationId);
}
