package com.stackforge.kidtrack.platform.notifications.domain.services;

import com.stackforge.kidtrack.platform.notifications.domain.model.aggregates.Notification;
import com.stackforge.kidtrack.platform.notifications.domain.model.queries.NotificationQueries.GetNotificationByIdQuery;
import com.stackforge.kidtrack.platform.notifications.domain.model.queries.NotificationQueries.GetNotificationsQuery;
import java.util.List;
import java.util.Optional;

/** Read side of the Notifications bounded context. */
public interface NotificationQueryService {

    List<Notification> handle(GetNotificationsQuery query);

    Optional<Notification> handle(GetNotificationByIdQuery query);
}
