package com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Child;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChildRepository extends JpaRepository<Child, UUID> {

    List<Child> findByOrganizationId(UUID organizationId);

    List<Child> findByOrganizationIdAndParentId(UUID organizationId, UUID parentId);

    List<Child> findByParentId(UUID parentId);

    long countByOrganizationId(UUID organizationId);
}
