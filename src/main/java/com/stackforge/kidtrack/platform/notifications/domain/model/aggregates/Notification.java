package com.stackforge.kidtrack.platform.notifications.domain.model.aggregates;

import com.stackforge.kidtrack.platform.notifications.domain.model.valueobjects.NotificationType;
import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A notice sent to the parents of a trip: a student boarded, the bus is close, a delay... */
@Entity
@Table(name = "notifications")
public class Notification extends AuditableEntity {

    public static final int MESSAGE_MAX_LENGTH = 500;

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = MESSAGE_MAX_LENGTH)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "trip_id")
    private UUID tripId;

    protected Notification() {
    }

    public Notification(UUID organizationId, NotificationType type, String message, Instant sentAt, boolean read,
                        UUID parentId, UUID tripId) {
        this(UUID.randomUUID(), organizationId, type, message, sentAt, read, parentId, tripId);
    }

    /** Creates a notification with a predefined identifier (used by the demo data seeder). */
    public Notification(UUID id, UUID organizationId, NotificationType type, String message, Instant sentAt,
                        boolean read, UUID parentId, UUID tripId) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("The notification message cannot be empty.");
        }
        if (message.trim().length() > MESSAGE_MAX_LENGTH) {
            throw new IllegalArgumentException("The notification message is too long.");
        }
        this.id = id;
        this.organizationId = organizationId;
        this.type = type;
        this.message = message.trim();
        this.sentAt = sentAt == null ? Instant.now() : sentAt;
        this.read = read;
        this.parentId = parentId;
        this.tripId = tripId;
    }

    public void markAsRead() {
        this.read = true;
    }

    public void markAsUnread() {
        this.read = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public boolean isRead() {
        return read;
    }

    public UUID getParentId() {
        return parentId;
    }

    public UUID getTripId() {
        return tripId;
    }
}
