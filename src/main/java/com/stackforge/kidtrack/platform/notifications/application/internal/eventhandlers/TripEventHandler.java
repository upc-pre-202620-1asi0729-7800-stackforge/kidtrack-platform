package com.stackforge.kidtrack.platform.notifications.application.internal.eventhandlers;

import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.DeleteNotificationsByTripCommand;
import com.stackforge.kidtrack.platform.notifications.domain.services.NotificationCommandService;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.TripDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Removes the notifications of a trip when the Trip context deletes it. */
@Component
public class TripEventHandler {

    private final NotificationCommandService commandService;

    public TripEventHandler(NotificationCommandService commandService) {
        this.commandService = commandService;
    }

    @EventListener
    public void on(TripDeleted event) {
        commandService.handle(new DeleteNotificationsByTripCommand(event.tripId()));
    }
}
