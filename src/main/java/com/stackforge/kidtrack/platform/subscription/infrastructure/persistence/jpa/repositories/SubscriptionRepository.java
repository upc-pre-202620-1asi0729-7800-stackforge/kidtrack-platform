package com.stackforge.kidtrack.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Subscription;
import com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects.SubscriptionState;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByOrganizationId(UUID organizationId);

    List<Subscription> findByOrganizationIdAndState(UUID organizationId, SubscriptionState state);
}
