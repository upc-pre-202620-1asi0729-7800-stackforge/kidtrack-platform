package com.stackforge.kidtrack.platform.notifications.infrastructure.persistence.seeding;

import com.stackforge.kidtrack.platform.notifications.domain.model.aggregates.Notification;
import com.stackforge.kidtrack.platform.notifications.domain.model.valueobjects.NotificationType;
import com.stackforge.kidtrack.platform.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;
import com.stackforge.kidtrack.platform.shared.infrastructure.persistence.SeedIds;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Loads a few notifications for the demo parents. */
@Component
@Order(6)
@ConditionalOnProperty(prefix = "kidtrack.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSeeder.class);

    private final NotificationRepository notificationRepository;

    public NotificationSeeder(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (notificationRepository.count() > 0) {
            return;
        }
        var now = Instant.now();
        notificationRepository.save(new Notification(SeedIds.ORGANIZATION, NotificationType.ABORDAJE,
                "Lucía Nery abordó el bus en Av. Universitaria cdra. 54.", now.minus(3, ChronoUnit.HOURS), true,
                SeedIds.PARENT_ROSITA, SeedIds.TRIP_ESTE));
        notificationRepository.save(new Notification(SeedIds.ORGANIZATION, NotificationType.LLEGADA,
                "El bus llegó a destino: I.E. San Marcos.", now.minus(2, ChronoUnit.HOURS), true,
                SeedIds.PARENT_ROSITA, SeedIds.TRIP_ESTE));
        notificationRepository.save(new Notification(SeedIds.ORGANIZATION, NotificationType.RETRASO,
                "La ruta tiene un retraso de unos 10 minutos por tráfico.", now.minus(30, ChronoUnit.MINUTES), false,
                SeedIds.PARENT_JORGE, SeedIds.TRIP_NORTE));
        LOGGER.info("Notification demo data ensured.");
    }
}
