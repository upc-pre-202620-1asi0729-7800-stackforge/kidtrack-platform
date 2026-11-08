package com.stackforge.kidtrack.platform.subscription.interfaces.rest.transform;

import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Dates;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Plan;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Subscription;
import com.stackforge.kidtrack.platform.subscription.interfaces.rest.resources.SubscriptionResources.PlanResource;
import com.stackforge.kidtrack.platform.subscription.interfaces.rest.resources.SubscriptionResources.SubscriptionResource;
import java.time.Instant;

/** Converts subscription aggregates into REST resources. */
public final class SubscriptionResourceAssembler {

    private SubscriptionResourceAssembler() {
    }

    public static PlanResource toResource(Plan plan) {
        return new PlanResource(plan.getId().toString(), plan.getTier().name(), plan.getName(), plan.getMaxRoutes(),
                plan.getMaxDrivers(), plan.getMaxStudents(), plan.getPrice(), plan.getDescription());
    }

    public static SubscriptionResource toResource(Subscription subscription, Plan plan) {
        var now = Instant.now();
        return new SubscriptionResource(
                subscription.getId().toString(),
                subscription.getOrganizationId().toString(),
                subscription.getPlanId().toString(),
                plan == null ? null : plan.getTier().name(),
                subscription.effectiveState(now).name(),
                Dates.format(subscription.getStartDate()),
                Dates.format(subscription.getEndDate()),
                subscription.remainingDays(now));
    }
}
