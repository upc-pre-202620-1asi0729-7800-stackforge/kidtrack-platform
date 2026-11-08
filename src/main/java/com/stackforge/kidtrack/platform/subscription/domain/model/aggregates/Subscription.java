package com.stackforge.kidtrack.platform.subscription.domain.model.aggregates;

import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects.SubscriptionState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** The subscription of an organization to a plan during a period of time. */
@Entity
@Table(name = "subscriptions")
public class Subscription extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionState state;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    protected Subscription() {
    }

    public Subscription(UUID organizationId, UUID planId, SubscriptionState state, Instant startDate,
                        Instant endDate) {
        this(UUID.randomUUID(), organizationId, planId, state, startDate, endDate);
    }

    /** Creates a subscription with a predefined identifier (used by the demo data seeder). */
    public Subscription(UUID id, UUID organizationId, UUID planId, SubscriptionState state, Instant startDate,
                        Instant endDate) {
        this.id = id;
        this.organizationId = organizationId;
        this.planId = planId;
        this.state = state == null ? SubscriptionState.ACTIVE : state;
        reschedule(startDate, endDate);
    }

    public void activate() {
        this.state = SubscriptionState.ACTIVE;
    }

    public void upgradeTo(UUID planId) {
        this.planId = planId;
    }

    public void cancel() {
        this.state = SubscriptionState.CANCELLED;
    }

    public void changeState(SubscriptionState state) {
        this.state = state;
    }

    public void reschedule(Instant startDate, Instant endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("The subscription dates are required.");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Subscription end date must be after the start date.");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /** The stored state, except that an active subscription whose period is over is reported as expired. */
    public SubscriptionState effectiveState(Instant now) {
        return state == SubscriptionState.ACTIVE && !endDate.isAfter(now) ? SubscriptionState.EXPIRED : state;
    }

    public boolean isActive(Instant now) {
        return effectiveState(now) == SubscriptionState.ACTIVE;
    }

    public int remainingDays(Instant now) {
        var remaining = Duration.between(now, endDate);
        if (remaining.isNegative() || remaining.isZero()) {
            return 0;
        }
        return (int) Math.ceil(remaining.toSeconds() / 86_400.0);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public SubscriptionState getState() {
        return state;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }
}
