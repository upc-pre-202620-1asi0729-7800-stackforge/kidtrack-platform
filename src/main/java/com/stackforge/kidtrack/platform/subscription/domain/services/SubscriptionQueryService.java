package com.stackforge.kidtrack.platform.subscription.domain.services;

import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Plan;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Subscription;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetAllPlansQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetPlanByIdQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetSubscriptionByIdQuery;
import com.stackforge.kidtrack.platform.subscription.domain.model.queries.SubscriptionQueries.GetSubscriptionsQuery;
import java.util.List;
import java.util.Optional;

/** Read side of the Subscription bounded context. */
public interface SubscriptionQueryService {

    List<Plan> handle(GetAllPlansQuery query);

    Optional<Plan> handle(GetPlanByIdQuery query);

    List<Subscription> handle(GetSubscriptionsQuery query);

    Optional<Subscription> handle(GetSubscriptionByIdQuery query);
}
