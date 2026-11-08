package com.stackforge.kidtrack.platform.subscription.domain.model.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Commands handled by the Subscription bounded context. */
public final class SubscriptionCommands {

    private SubscriptionCommands() {
    }

    public record CreatePlanCommand(String planTier, String name, int maxRoutes, int maxDrivers, int maxStudents,
                                    BigDecimal price, String description) {
    }

    public record CreateSubscriptionCommand(UUID organizationId, UUID planId, String state, Instant startDate,
                                            Instant endDate) {
    }

    /** Partial update of a subscription; {@code null} fields are left untouched. */
    public record UpdateSubscriptionCommand(UUID subscriptionId, UUID planId, String state, Instant startDate,
                                            Instant endDate) {
    }
}
