package com.stackforge.kidtrack.platform.iam.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.Organization;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
