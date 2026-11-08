package com.stackforge.kidtrack.platform.subscription.application.internal.queryservices;

import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Plan;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Subscription;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetAllPlansQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetPlanByIdQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetSubscriptionByIdQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetSubscriptionsQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects.SubscriptionState;
import com.stackforge.kidtrack.platform.subscription.domain.services.SubscriptionQueryService;
import com.stackforge.kidtrack.platform.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.stackforge.kidtrack.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionQueryServiceImpl(PlanRepository planRepository,
                                        SubscriptionRepository subscriptionRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public List<Plan> handle(GetAllPlansQuery query) {
        return planRepository.findAll(Sort.by("price"));
    }

    @Override
    public Optional<Plan> handle(GetPlanByIdQuery query) {
        return planRepository.findById(query.planId());
    }

    @Override
    public List<Subscription> handle(GetSubscriptionsQuery query) {
        var now = Instant.now();
        var subscriptions = query.organizationId() == null
                ? subscriptionRepository.findAll()
                : subscriptionRepository.findByOrganizationId(query.organizationId());
        var state = query.state() == null || query.state().isBlank() ? null : SubscriptionState.from(query.state());
        return subscriptions.stream()
                .filter(subscription -> state == null || subscription.effectiveState(now) == state)
                .sorted(Comparator.comparing(Subscription::getStartDate).reversed())
                .toList();
    }

    @Override
    public Optional<Subscription> handle(GetSubscriptionByIdQuery query) {
        return subscriptionRepository.findById(query.subscriptionId());
    }
}
