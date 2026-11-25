package com.stackforge.kidtrack.platform.trip.domain.model.aggregates;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.BusinessRuleException;
import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.StringListConverter;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.EmergencyType;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentSeverity;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentStatus;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentType;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentUpdate;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    /** Set only for force majeure emergencies. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "emergency_type", length = 20)
    private EmergencyType emergencyType;

    @Column(length = 200)
    private String location;

    /** Students aboard when the emergency happened: their parents are the ones informed. */
    @Convert(converter = StringListConverter.class)
    @Column(name = "student_ids", length = 4000)
    private List<String> studentIds = new ArrayList<>();

    /** Whether the driver confirmed that the students are fine; {@code null} while unknown. */
    @Column(name = "students_safe")
    private Boolean studentsSafe;

    @Column(name = "service_contacted_at")
    private Instant serviceContactedAt;

    @Column(name = "service_contacted_by", length = 200)
    private String serviceContactedBy;

    @Column(length = 300)
    private String resolution;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "incident_updates", joinColumns = @JoinColumn(name = "incident_id"))
    @OrderColumn(name = "update_order")
    private List<IncidentUpdate> updates = new ArrayList<>();

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

    /** Closes the incident, keeping a note of how it ended. */
    public void resolve(String resolution) {
        var value = resolution == null ? "" : resolution.trim();
        if (value.length() > 300) {
            throw new IllegalArgumentException("The resolution is too long.");
        }
        this.status = IncidentStatus.RESOLVED;
        this.resolution = value;
        this.resolvedAt = Instant.now();
    }

    public void reopen() {
        this.status = IncidentStatus.OPEN;
        this.resolution = null;
        this.resolvedAt = null;
    }

    /** Marks the incident as a force majeure emergency. */
    public void declareEmergency(EmergencyType emergencyType, String location, Boolean studentsSafe,
                                 List<String> studentIds) {
        var place = location == null ? "" : location.trim();
        if (place.length() > 200) {
            throw new IllegalArgumentException("The location is too long.");
        }
        this.emergencyType = emergencyType;
        this.type = IncidentType.EMERGENCIA;
        this.severity = IncidentSeverity.HIGH;
        this.location = place;
        this.studentsSafe = studentsSafe;
        this.studentIds = new ArrayList<>(studentIds);
    }

    /** Records that the external service that attends this emergency was called. */
    public void recordServiceCall(String contactedBy) {
        requireOpenEmergency();
        if (serviceContactedAt != null) {
            throw new BusinessRuleException("The call to the emergency service is already recorded.");
        }
        this.serviceContactedAt = Instant.now();
        this.serviceContactedBy = contactedBy;
    }

    /** Adds news about the emergency, optionally confirming the state of the students. */
    public IncidentUpdate addUpdate(String message, Boolean studentsSafe, String author) {
        requireOpenEmergency();
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("The update message cannot be empty.");
        }
        if (message.trim().length() > 300) {
            throw new IllegalArgumentException("The update message is too long.");
        }
        if (studentsSafe != null) {
            this.studentsSafe = studentsSafe;
        }
        var update = new IncidentUpdate(message.trim(), Instant.now(), author);
        this.updates.add(update);
        return update;
    }

    public boolean isEmergency() {
        return emergencyType != null;
    }

    private void requireOpenEmergency() {
        if (!isEmergency()) {
            throw new BusinessRuleException("The incident is not an emergency.");
        }
        if (status != IncidentStatus.OPEN) {
            throw new BusinessRuleException("The emergency is already closed.");
        }
    }

    public EmergencyType getEmergencyType() {
        return emergencyType;
    }

    public String getLocation() {
        return location;
    }

    public List<String> getStudentIds() {
        return studentIds == null ? List.of() : studentIds;
    }

    public Boolean getStudentsSafe() {
        return studentsSafe;
    }

    public Instant getServiceContactedAt() {
        return serviceContactedAt;
    }

    public String getServiceContactedBy() {
        return serviceContactedBy;
    }

    public String getResolution() {
        return resolution;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public List<IncidentUpdate> getUpdates() {
        return updates;
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
