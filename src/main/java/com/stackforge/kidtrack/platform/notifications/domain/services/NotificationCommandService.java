package com.stackforge.kidtrack.platform.notifications.domain.services;

import com.stackforge.kidtrack.platform.notifications.domain.model.aggregates.Notification;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.CreateNotificationCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.DeleteNotificationCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.DeleteNotificationsByTripCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.MarkNotificationReadCommand;

/** Write side of the Notifications bounded context. */
public interface NotificationCommandService {

    Notification handle(CreateNotificationCommand command);

    Notification handle(MarkNotificationReadCommand command);

    void handle(DeleteNotificationCommand command);

    void handle(DeleteNotificationsByTripCommand command);
}
