package com.stackforge.kidtrack.platform.trip.domain.model.aggregates;

import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentSeverity;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentStatus;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** An incident reported by a driver (delay, breakdown, accident, emergency...), optionally tied to a trip. */
@Entity
@Table(name = "incidents")
public class Incident extends AuditableEntity {

    public static final int DESCRIPTION_MIN_LENGTH = 3;
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "route_id")
    private UUID routeId;

    @Column(name = "route_name", length = 150)
    private String routeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentSeverity severity;

    @Column(nullable = false, length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "reported_by", length = 200)
    private String reportedBy;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    protected Incident() {
    }

    public Incident(UUID organizationId, UUID tripId, UUID routeId, String routeName, IncidentType type,
                    IncidentSeverity severity, String description, String reportedBy, Instant reportedAt) {
        this(UUID.randomUUID(), organizationId, tripId, routeId, routeName, type, severity, description, reportedBy,
                reportedAt);
    }

    /** Creates an incident with a predefined identifier (used by the demo data seeder). */
    public Incident(UUID id, UUID organizationId, UUID tripId, UUID routeId, String routeName, IncidentType type,
                    IncidentSeverity severity, String description, String reportedBy, Instant reportedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.tripId = tripId;
        this.routeId = routeId;
        this.routeName = routeName == null ? "" : routeName;
        this.type = type == null ? IncidentType.OTRO : type;
        this.severity = severity == null ? IncidentSeverity.LOW : severity;
        this.description = validateDescription(description);
        this.reportedBy = reportedBy == null ? "" : reportedBy;
        this.reportedAt = reportedAt == null ? Instant.now() : reportedAt;
        this.status = IncidentStatus.OPEN;
    }

    public void resolve() {
        this.status = IncidentStatus.RESOLVED;
    }

    public void reopen() {
        this.status = IncidentStatus.OPEN;
    }

    private static String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("The incident description cannot be empty.");
        }
        var value = description.trim();
        if (value.length() < DESCRIPTION_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "The incident description is too short (minimum " + DESCRIPTION_MIN_LENGTH + " characters).");
        }
        if (value.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "The incident description cannot exceed " + DESCRIPTION_MAX_LENGTH + " characters.");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getTripId() {
        return tripId;
    }

    public UUID getRouteId() {
        return routeId;
    }

    public String getRouteName() {
        return routeName;
    }

    public IncidentType getType() {
        return type;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public IncidentStatus getStatus() {
        return status;
    }
}
