package com.stackforge.kidtrack.platform.notifications.application.internal.eventhandlers;

import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.CreateNotificationCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.DeleteNotificationsByTripCommand;
import com.stackforge.kidtrack.platform.notifications.domain.services.NotificationCommandService;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.acl.StakeholderContextFacade.ChildContact;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.acl.StakeholderContextFacade;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.EmergencyNotice;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.EmergencyStage;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.StudentHandedOver;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.StudentNotDelivered;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.TripDeleted;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Informs the parents about what happens on a trip, and removes the notifications of deleted trips. */
@Component
public class TripEventHandler {

    private final NotificationCommandService commandService;

    private final StakeholderContextFacade stakeholderFacade;

    public TripEventHandler(NotificationCommandService commandService, StakeholderContextFacade stakeholderFacade) {
        this.commandService = commandService;
        this.stakeholderFacade = stakeholderFacade;
    }

    @EventListener
    public void on(StudentHandedOver event) {
        stakeholderFacade.childContact(event.childId()).ifPresent(child -> send(event.organizationId(), "ENTREGA",
                ParentMessages.handedOver(child.name(), event.receiverName(), event.relationship()),
                child.parentId(), event.tripId()));
    }

    @EventListener
    public void on(StudentNotDelivered event) {
        stakeholderFacade.childContact(event.childId()).ifPresent(child -> send(event.organizationId(),
                "NO_ENTREGADO",
                ParentMessages.notDelivered(child.name(), event.driverName(), event.vehiclePlate()),
                child.parentId(), event.tripId()));
    }

    /** One message per parent, naming their own children aboard. */
    @EventListener
    public void on(EmergencyNotice notice) {
        var byParent = notice.studentIds().stream()
                .map(UUID::fromString)
                .map(stakeholderFacade::childContact)
                .flatMap(Optional::stream)
                .filter(child -> Objects.nonNull(child.parentId()))
                .collect(Collectors.groupingBy(ChildContact::parentId, LinkedHashMap::new, Collectors.toList()));
        var type = notice.stage() == EmergencyStage.DECLARED ? "EMERGENCIA" : "ACTUALIZACION";
        byParent.forEach((parentId, children) -> {
            List<String> names = children.stream().map(ChildContact::name).toList();
            send(notice.organizationId(), type, ParentMessages.emergency(notice, names), parentId, notice.tripId());
        });
    }

    private void send(UUID organizationId, String type, String message, UUID parentId, UUID tripId) {
        if (parentId != null) {
            commandService.handle(new CreateNotificationCommand(organizationId, type, message, null, false, parentId,
                    tripId));
        }
    }

    @EventListener
    public void on(TripDeleted event) {
        commandService.handle(new DeleteNotificationsByTripCommand(event.tripId()));
    }
}
