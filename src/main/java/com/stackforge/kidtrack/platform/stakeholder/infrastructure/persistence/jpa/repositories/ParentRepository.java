package com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Parent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {

    List<Parent> findByOrganizationId(UUID organizationId);

    boolean existsByOrganizationIdAndEmail(UUID organizationId, String email);
}
