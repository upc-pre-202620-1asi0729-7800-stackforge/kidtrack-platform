package com.stackforge.kidtrack.platform.notifications.domain.model.commands;

import java.util.UUID;

/** Commands handled by the Notifications bounded context. */
public final class NotificationCommands {

    private NotificationCommands() {
    }

    public record CreateNotificationCommand(UUID organizationId, String type, String message, String timestamp,
                                            Boolean read, UUID parentId, UUID tripId) {
    }

    public record MarkNotificationReadCommand(UUID notificationId, boolean read) {
    }

    public record DeleteNotificationCommand(UUID notificationId) {
    }

    /** Removes every notification that refers to a trip (the trip was deleted). */
    public record DeleteNotificationsByTripCommand(UUID tripId) {
    }
}
