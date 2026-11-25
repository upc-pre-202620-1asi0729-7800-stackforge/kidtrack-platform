package com.stackforge.kidtrack.platform.stakeholder.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.AuthorizedPerson;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizedPersonRepository extends JpaRepository<AuthorizedPerson, UUID> {

    List<AuthorizedPerson> findByOrganizationId(UUID organizationId);

    List<AuthorizedPerson> findByChildId(UUID childId);

    boolean existsByChildIdAndDocumentNumber(UUID childId, String documentNumber);
}
