package com.stackforge.kidtrack.platform.notifications.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/** REST resources of the Notifications bounded context. Identifiers travel as text, like in the web application. */
public final class NotificationResources {

    private NotificationResources() {
    }

    public record NotificationResource(String id, String type, String message, String timestamp, boolean read,
                                       String parentId, String tripId, String organizationId) {
    }

    public record NotificationRequestResource(@NotBlank String type, @NotBlank String message, String timestamp,
                                              Boolean read, String parentId, String tripId, String organizationId) {
    }

    /** Partial update of a notification: whether it was read. */
    public record NotificationPatchResource(Boolean read) {
    }
}
