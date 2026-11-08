package com.stackforge.kidtrack.platform.subscription.interfaces.acl;

import java.util.Optional;
import java.util.UUID;

/** Anti-corruption facade that exposes the quotas of the plan an organization is subscribed to. */
public interface SubscriptionContextFacade {

    /** Quotas granted by a plan. */
    record PlanLimits(int maxRoutes, int maxDrivers, int maxStudents) {
    }

    /**
     * Quotas of the active subscription of the organization, or empty when it has none (in which case the
     * other contexts do not restrict it).
     */
    Optional<PlanLimits> activePlanLimits(UUID organizationId);
}
