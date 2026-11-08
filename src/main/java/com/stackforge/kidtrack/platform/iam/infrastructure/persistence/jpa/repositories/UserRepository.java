package com.stackforge.kidtrack.platform.iam.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.User;
import com.stackforge.kidtrack.platform.iam.domain.model.valueobjects.RoleTier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailValue(String email);

    boolean existsByEmailValue(String email);

    List<User> findByOrganizationId(UUID organizationId);

    boolean existsByOrganizationIdAndRole(UUID organizationId, RoleTier role);
}
