package com.stackforge.kidtrack.platform.trip.domain.model.aggregates;

import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.HandoverOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Who received a student during a trip, or the fact that nobody registered was there to receive them. */
@Entity
@Table(name = "handovers")
public class Handover extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "child_name", length = 200)
    private String childName;

    @Column(name = "authorized_person_id")
    private UUID authorizedPersonId;

    @Column(name = "receiver_name", length = 150)
    private String receiverName;

    @Column(length = 50)
    private String relationship;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private HandoverOutcome outcome;

    @Column(name = "recorded_by", length = 200)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected Handover() {
    }

    private Handover(UUID organizationId, UUID tripId, UUID childId, String childName, UUID authorizedPersonId,
                     String receiverName, String relationship, HandoverOutcome outcome, String recordedBy) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.tripId = tripId;
        this.childId = childId;
        this.childName = childName;
        this.authorizedPersonId = authorizedPersonId;
        this.receiverName = receiverName == null ? "" : receiverName.trim();
        this.relationship = relationship == null ? "" : relationship;
        this.outcome = outcome;
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
        if (this.receiverName.length() > 150) {
            throw new IllegalArgumentException("The name of the receiver is too long.");
        }
    }

    /** The student was received by a registered person. */
    public static Handover delivered(UUID organizationId, UUID tripId, UUID childId, String childName,
                                     UUID authorizedPersonId, String receiverName, String relationship,
                                     String recordedBy) {
        return new Handover(organizationId, tripId, childId, childName, authorizedPersonId, receiverName,
                relationship, HandoverOutcome.DELIVERED, recordedBy);
    }

    /** Nobody registered was there: the student is not handed over. */
    public static Handover notDelivered(UUID organizationId, UUID tripId, UUID childId, String childName,
                                        String attemptedReceiverName, String recordedBy) {
        return new Handover(organizationId, tripId, childId, childName, null, attemptedReceiverName, null,
                HandoverOutcome.NOT_DELIVERED, recordedBy);
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

    public UUID getChildId() {
        return childId;
    }

    public String getChildName() {
        return childName;
    }

    public UUID getAuthorizedPersonId() {
        return authorizedPersonId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getRelationship() {
        return relationship;
    }

    public HandoverOutcome getOutcome() {
        return outcome;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
