package com.stackforge.kidtrack.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Plan;
import com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects.PlanTier;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    boolean existsByTier(PlanTier tier);
}
