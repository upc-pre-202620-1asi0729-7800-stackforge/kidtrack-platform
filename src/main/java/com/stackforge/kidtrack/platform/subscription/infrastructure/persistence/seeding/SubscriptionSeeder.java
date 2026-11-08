package com.stackforge.kidtrack.platform.subscription.infrastructure.persistence.seeding;

import com.stackforge.kidtrack.platform.shared.infrastructure.persistence.SeedIds;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Plan;
import com.stackforge.kidtrack.platform.subscription.domain.model.aggregates.Subscription;
import com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects.PlanTier;
import com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects.SubscriptionState;
import com.stackforge.kidtrack.platform.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.stackforge.kidtrack.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Loads the three commercial plans and an active subscription of the demo organization. */
@Component
@Order(2)
@ConditionalOnProperty(prefix = "kidtrack.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionSeeder.class);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionSeeder(PlanRepository planRepository, SubscriptionRepository subscriptionRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (planRepository.count() == 0) {
            planRepository.save(new Plan(SeedIds.PLAN_BASIC, PlanTier.BASIC, "Básico", 2, 2, 30,
                    new BigDecimal("9.99"), "Ideal para grupos pequeños de padres organizados"));
            planRepository.save(new Plan(SeedIds.PLAN_INTERMEDIATE, PlanTier.INTERMEDIATE, "Intermedio", 6, 6, 100,
                    new BigDecimal("24.99"), "Para transportistas con varias rutas y conductores"));
            planRepository.save(new Plan(SeedIds.PLAN_COMPLETE, PlanTier.COMPLETE, "Completo", 20, 20, 300,
                    new BigDecimal("49.99"), "Para empresas de transporte escolar con operación a gran escala"));
        }
        if (subscriptionRepository.count() == 0) {
            var now = Instant.now();
            subscriptionRepository.save(new Subscription(SeedIds.SUBSCRIPTION, SeedIds.ORGANIZATION,
                    SeedIds.PLAN_COMPLETE, SubscriptionState.ACTIVE, now, now.plus(Duration.ofDays(30))));
        }
        LOGGER.info("Subscription demo data ensured.");
    }
}
