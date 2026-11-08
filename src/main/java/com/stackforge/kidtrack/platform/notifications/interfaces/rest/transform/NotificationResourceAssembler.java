package com.stackforge.kidtrack.platform.notifications.interfaces.rest.transform;

import com.stackforge.kidtrack.platform.notifications.domain.model.aggregates.Notification;
import com.stackforge.kidtrack.platform.notifications.interfaces.rest.resources.NotificationResources.NotificationResource;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Dates;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;

/** Converts notification aggregates into REST resources. */
public final class NotificationResourceAssembler {

    private NotificationResourceAssembler() {
    }

    public static NotificationResource toResource(Notification notification) {
        return new NotificationResource(
                notification.getId().toString(),
                notification.getType().name(),
                notification.getMessage(),
                Dates.format(notification.getSentAt()),
                notification.isRead(),
                Ids.asText(notification.getParentId()),
                Ids.asText(notification.getTripId()),
                notification.getOrganizationId().toString());
    }
}
