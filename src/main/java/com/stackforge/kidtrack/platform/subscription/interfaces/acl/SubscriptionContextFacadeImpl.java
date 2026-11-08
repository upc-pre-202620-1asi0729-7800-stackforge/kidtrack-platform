package com.stackforge.kidtrack.platform.subscription.interfaces.acl;

import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Subscription;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetPlanByIdQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetSubscriptionsQuery;
import com.stackforge.kidtrack.platform.subscription.domain.services.SubscriptionQueryService;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionContextFacadeImpl implements SubscriptionContextFacade {

    private final SubscriptionQueryService queryService;

    public SubscriptionContextFacadeImpl(SubscriptionQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public Optional<PlanLimits> activePlanLimits(UUID organizationId) {
        if (organizationId == null) {
            return Optional.empty();
        }
        return queryService.handle(new GetSubscriptionsQuery(organizationId, "ACTIVE")).stream()
                .max(Comparator.comparing(Subscription::getEndDate))
                .flatMap(subscription -> queryService.handle(new GetPlanByIdQuery(subscription.getPlanId())))
                .map(plan -> new PlanLimits(plan.getMaxRoutes(), plan.getMaxDrivers(), plan.getMaxStudents()));
    }
}
