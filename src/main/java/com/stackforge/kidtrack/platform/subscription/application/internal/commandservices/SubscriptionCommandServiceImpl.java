package com.stackforge.kidtrack.platform.subscription.application.internal.commandservices;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.ConflictException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Plan;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Subscription;
import com.stackforge.kidtrack.platform.subscription.domain.model.commands.SubscriptionCommands.CreatePlanCommand;
import com.stackforge.kidtrack.platform.subscription.domain.model.commands.SubscriptionCommands.CreateSubscriptionCommand;
import com.stackforge.kidtrack.platform.subscription.domain.model.commands.SubscriptionCommands.UpdateSubscriptionCommand;
import com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects.PlanTier;
import com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects.SubscriptionState;
import com.stackforge.kidtrack.platform.subscription.domain.services.SubscriptionCommandService;
import com.stackforge.kidtrack.platform.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.stackforge.kidtrack.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

    private static final Duration DEFAULT_PERIOD = Duration.ofDays(30);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionCommandServiceImpl(PlanRepository planRepository,
                                          SubscriptionRepository subscriptionRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Plan handle(CreatePlanCommand command) {
        var tier = PlanTier.from(command.planTier());
        if (planRepository.existsByTier(tier)) {
            throw new ConflictException("A plan with tier " + tier + " already exists.");
        }
        return planRepository.save(new Plan(tier, command.name(), command.maxRoutes(), command.maxDrivers(),
                command.maxStudents(), command.price(), command.description()));
    }

    /**
     * Creates a subscription. An organization has at most one active subscription, so subscribing to a plan
     * cancels the one that was active before.
     */
    @Override
    public Subscription handle(CreateSubscriptionCommand command) {
        requirePlan(command.planId());
        var state = SubscriptionState.from(command.state());
        var start = command.startDate() != null ? command.startDate() : Instant.now();
        var end = command.endDate() != null ? command.endDate() : start.plus(DEFAULT_PERIOD);
        var subscription = new Subscription(command.organizationId(), command.planId(), state, start, end);
        if (state == SubscriptionState.ACTIVE) {
            cancelActiveSubscriptions(command.organizationId());
        }
        return subscriptionRepository.save(subscription);
    }

    @Override
    public Subscription handle(UpdateSubscriptionCommand command) {
        var subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new NotFoundException("Subscription was not found."));
        if (command.planId() != null) {
            requirePlan(command.planId());
            subscription.upgradeTo(command.planId());
        }
        if (command.startDate() != null || command.endDate() != null) {
            subscription.reschedule(
                    command.startDate() != null ? command.startDate() : subscription.getStartDate(),
                    command.endDate() != null ? command.endDate() : subscription.getEndDate());
        }
        if (command.state() != null) {
            var state = SubscriptionState.from(command.state());
            if (state == SubscriptionState.ACTIVE) {
                cancelActiveSubscriptions(subscription.getOrganizationId(), subscription);
                subscription.activate();
            } else {
                subscription.changeState(state);
            }
        }
        return subscriptionRepository.save(subscription);
    }

    private void requirePlan(java.util.UUID planId) {
        if (!planRepository.existsById(planId)) {
            throw new NotFoundException("Plan was not found.");
        }
    }

    private void cancelActiveSubscriptions(java.util.UUID organizationId) {
        cancelActiveSubscriptions(organizationId, null);
    }

    private void cancelActiveSubscriptions(java.util.UUID organizationId, Subscription except) {
        subscriptionRepository.findByOrganizationIdAndState(organizationId, SubscriptionState.ACTIVE).stream()
                .filter(active -> except == null || !active.getId().equals(except.getId()))
                .forEach(active -> {
                    active.cancel();
                    subscriptionRepository.save(active);
                });
    }
}
