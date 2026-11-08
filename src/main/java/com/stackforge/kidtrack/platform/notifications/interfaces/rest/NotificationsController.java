package com.stackforge.kidtrack.platform.notifications.interfaces.rest;

import com.stackforge.kidtrack.platform.notifications.domain.model.aggregates.Notification;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.CreateNotificationCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.DeleteNotificationCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.commands.NotificationCommands.MarkNotificationReadCommand;
import com.stackforge.kidtrack.platform.notifications.domain.model.queries.NotificationQueries.GetNotificationByIdQuery;
import com.stackforge.kidtrack.platform.notifications.domain.model.queries.NotificationQueries.GetNotificationsQuery;
import com.stackforge.kidtrack.platform.notifications.domain.services.NotificationCommandService;
import com.stackforge.kidtrack.platform.notifications.domain.services.NotificationQueryService;
import com.stackforge.kidtrack.platform.notifications.interfaces.rest.resources.NotificationResources.NotificationPatchResource;
import com.stackforge.kidtrack.platform.notifications.interfaces.rest.resources.NotificationResources.NotificationRequestResource;
import com.stackforge.kidtrack.platform.notifications.interfaces.rest.resources.NotificationResources.NotificationResource;
import com.stackforge.kidtrack.platform.notifications.interfaces.rest.transform.NotificationResourceAssembler;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notices sent to the parents about the trips of their children")
public class NotificationsController {

    private final NotificationCommandService commandService;
    private final NotificationQueryService queryService;

    public NotificationsController(NotificationCommandService commandService,
                                   NotificationQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the notifications",
            description = "Newest first. Optional filters: parentId, tripId and read.")
    public List<NotificationResource> getNotifications(@RequestParam(required = false) String organizationId,
                                                       @RequestParam(required = false) String parentId,
                                                       @RequestParam(required = false) String tripId,
                                                       @RequestParam(required = false) Boolean read,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        return queryService.handle(new GetNotificationsQuery(scope, Ids.parseOrNull(parentId),
                Ids.parseOrNull(tripId), read)).stream().map(NotificationResourceAssembler::toResource).toList();
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get a notification by id")
    public NotificationResource getNotificationById(@PathVariable UUID notificationId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return NotificationResourceAssembler.toResource(findInScope(notificationId, jwt));
    }

    @PostMapping
    @Operation(summary = "Send a notification", description = "Administrators and drivers.")
    public ResponseEntity<NotificationResource> createNotification(
            @Valid @RequestBody NotificationRequestResource resource, @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAnyRole(caller, "ADMIN", "DRIVER");
        var organizationId = Caller.requireScope(caller, Ids.parseOrNull(resource.organizationId()));
        var notification = commandService.handle(new CreateNotificationCommand(organizationId, resource.type(),
                resource.message(), resource.timestamp(), resource.read(), Ids.parseOrNull(resource.parentId()),
                Ids.parseOrNull(resource.tripId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationResourceAssembler.toResource(notification));
    }

    @PatchMapping("/{notificationId}")
    @Operation(summary = "Mark a notification as read (or unread)")
    public NotificationResource patchNotification(@PathVariable UUID notificationId,
                                                  @RequestBody NotificationPatchResource resource,
                                                  @AuthenticationPrincipal Jwt jwt) {
        findInScope(notificationId, jwt);
        if (resource.read() == null) {
            throw new IllegalArgumentException("read is required.");
        }
        return NotificationResourceAssembler.toResource(
                commandService.handle(new MarkNotificationReadCommand(notificationId, resource.read())));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID notificationId,
                                                   @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(notificationId, jwt);
        commandService.handle(new DeleteNotificationCommand(notificationId));
        return ResponseEntity.noContent().build();
    }

    private Notification findInScope(UUID notificationId, Jwt jwt) {
        var notification = queryService.handle(new GetNotificationByIdQuery(notificationId))
                .orElseThrow(() -> new NotFoundException("Notification was not found."));
        var caller = Caller.from(jwt);
        if (caller != null && caller.organizationId() != null
                && !caller.organizationId().equals(notification.getOrganizationId())) {
            throw new AccessDeniedException("The notification belongs to another organization.");
        }
        return notification;
    }
}
