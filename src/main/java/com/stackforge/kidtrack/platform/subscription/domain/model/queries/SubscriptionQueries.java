package com.stackforge.kidtrack.platform.subscription.domain.model.queries;

import java.util.UUID;

/** Queries handled by the Subscription bounded context. */
public final class SubscriptionQueries {

    private SubscriptionQueries() {
    }

    public record GetAllPlansQuery() {
    }

    public record GetPlanByIdQuery(UUID planId) {
    }

    /** Lists the subscriptions of an organization, optionally restricted to a state (ACTIVE, CANCELLED...). */
    public record GetSubscriptionsQuery(UUID organizationId, String state) {
    }

    public record GetSubscriptionByIdQuery(UUID subscriptionId) {
    }
}
