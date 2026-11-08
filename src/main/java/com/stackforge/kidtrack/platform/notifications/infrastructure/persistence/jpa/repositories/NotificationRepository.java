package com.stackforge.kidtrack.platform.notifications.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.notifications.domain.model.aggregates.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByOrganizationId(UUID organizationId);

    List<Notification> findByTripId(UUID tripId);
}
