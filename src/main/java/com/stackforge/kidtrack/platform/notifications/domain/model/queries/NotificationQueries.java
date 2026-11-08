package com.stackforge.kidtrack.platform.notifications.domain.model.queries;

import java.util.UUID;

/** Queries handled by the Notifications bounded context. */
public final class NotificationQueries {

    private NotificationQueries() {
    }

    /** Lists the notifications of an organization, newest first; every other criterion is optional. */
    public record GetNotificationsQuery(UUID organizationId, UUID parentId, UUID tripId, Boolean read) {
    }

    public record GetNotificationByIdQuery(UUID notificationId) {
    }
}
