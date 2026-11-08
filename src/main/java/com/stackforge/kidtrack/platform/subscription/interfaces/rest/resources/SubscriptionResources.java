package com.stackforge.kidtrack.platform.subscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/** REST resources of the Subscription bounded context. */
public final class SubscriptionResources {

    private SubscriptionResources() {
    }

    public record PlanResource(String id, String planTier, String name, int maxRoutes, int maxDrivers,
                               int maxStudents, BigDecimal price, String description) {
    }

    public record CreatePlanResource(@NotBlank String planTier, @NotBlank String name, int maxRoutes,
                                     int maxDrivers, int maxStudents, BigDecimal price, String description) {
    }

    public record SubscriptionResource(String id, String organizationId, String planId, String planTier,
                                       String state, String startDate, String endDate, int remainingDays) {
    }

    /** {@code planTier} is accepted because the web application sends it, but the plan is identified by id. */
    public record CreateSubscriptionResource(String organizationId, @NotBlank String planId, String planTier,
                                             String state, String startDate, String endDate) {
    }

    public record UpdateSubscriptionResource(String planId, String planTier, String state, String startDate,
                                             String endDate) {
    }
}
