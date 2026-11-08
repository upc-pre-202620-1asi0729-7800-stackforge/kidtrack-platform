package com.stackforge.kidtrack.platform.notifications.application.internal.commandservices;

import com.stackforge.kidtrack.platform.notifications.domain.model.aggregates.Notification;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.CreateNotificationCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.DeleteNotificationCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.DeleteNotificationsByTripCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.MarkNotificationReadCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.valueobjects.NotificationType;
import com.stackforge.kidtrack.platform.notifications.domain.services.NotificationCommandService;
import com.stackforge.kidtrack.platform.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Dates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;

    public NotificationCommandServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification handle(CreateNotificationCommand command) {
        var notification = new Notification(command.organizationId(), NotificationType.from(command.type()),
                command.message(), Dates.parseInstant(command.timestamp()),
                command.read() != null && command.read(), command.parentId(), command.tripId());
        return notificationRepository.save(notification);
    }

    @Override
    public Notification handle(MarkNotificationReadCommand command) {
        var notification = find(command.notificationId());
        if (command.read()) {
            notification.markAsRead();
        } else {
            notification.markAsUnread();
        }
        return notificationRepository.save(notification);
    }

    @Override
    public void handle(DeleteNotificationCommand command) {
        notificationRepository.delete(find(command.notificationId()));
    }

    @Override
    public void handle(DeleteNotificationsByTripCommand command) {
        notificationRepository.deleteAll(notificationRepository.findByTripId(command.tripId()));
    }

    private Notification find(java.util.UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification was not found."));
    }
}
