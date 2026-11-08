package com.stackforge.kidtrack.platform.notifications.application.internal.queryservices;

import com.stackforge.kidtrack.platform.notifications.domain.model.aggregates.Notification;
import com.stackforge.kidtrack.platform.notifications.domain.model.queries.NotificationQueries.GetNotificationByIdQuery;
import com.stackforge.kidtrack.platform.notifications.domain.model.queries.NotificationQueries.GetNotificationsQuery;
import com.stackforge.kidtrack.platform.notifications.domain.services.NotificationQueryService;
import com.stackforge.kidtrack.platform.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public NotificationQueryServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> handle(GetNotificationsQuery query) {
        var notifications = query.organizationId() == null
                ? notificationRepository.findAll() : notificationRepository.findByOrganizationId(query.organizationId());
        return notifications.stream()
                .filter(notification -> query.parentId() == null || query.parentId().equals(notification.getParentId()))
                .filter(notification -> query.tripId() == null || query.tripId().equals(notification.getTripId()))
                .filter(notification -> query.read() == null || notification.isRead() == query.read())
                .sorted(Comparator.comparing(Notification::getSentAt).reversed())
                .toList();
    }

    @Override
    public Optional<Notification> handle(GetNotificationByIdQuery query) {
        return notificationRepository.findById(query.notificationId());
    }
}
