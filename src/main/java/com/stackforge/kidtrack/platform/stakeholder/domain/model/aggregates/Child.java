package com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates;

import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.valueobjects.BoardingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A student that uses the school transport. It belongs to a parent and is picked up and dropped off on a route. */
@Entity
@Table(name = "children")
public class Child extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String grade;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "boarding_status", nullable = false, length = 20)
    private BoardingStatus boardingStatus;

    @Column(name = "has_photo", nullable = false)
    private boolean hasPhoto;

    protected Child() {
    }

    public Child(UUID organizationId, UUID parentId, String name, String grade, boolean active,
                 BoardingStatus boardingStatus, boolean hasPhoto) {
        this(UUID.randomUUID(), organizationId, parentId, name, grade, active, boardingStatus, hasPhoto);
    }

    /** Creates a child with a predefined identifier (used by the demo data seeder). */
    public Child(UUID id, UUID organizationId, UUID parentId, String name, String grade, boolean active,
                 BoardingStatus boardingStatus, boolean hasPhoto) {
        this.id = id;
        this.organizationId = organizationId;
        this.parentId = parentId;
        this.name = validateName(name);
        this.grade = grade == null ? "" : grade.trim();
        this.active = active;
        this.boardingStatus = boardingStatus == null ? BoardingStatus.EN_ESPERA : boardingStatus;
        this.hasPhoto = hasPhoto;
    }

    public void update(UUID parentId, String name, String grade, boolean active, boolean hasPhoto) {
        this.parentId = parentId;
        this.name = validateName(name);
        this.grade = grade == null ? "" : grade.trim();
        this.active = active;
        this.hasPhoto = hasPhoto;
    }

    public void assignParent(UUID parentId) {
        this.parentId = parentId;
    }

    public void detachFromParent() {
        this.parentId = null;
    }

    public void changeBoardingStatus(BoardingStatus status) {
        this.boardingStatus = status;
    }

    public void markBoarded() {
        this.boardingStatus = BoardingStatus.ABORDADO;
    }

    public void markAbsent() {
        this.boardingStatus = BoardingStatus.AUSENTE;
    }

    public void resetBoarding() {
        this.boardingStatus = BoardingStatus.EN_ESPERA;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The name of the student cannot be empty.");
        }
        var value = name.trim();
        if (value.length() > 200) {
            throw new IllegalArgumentException("The name of the student is too long.");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public String getGrade() {
        return grade;
    }

    public boolean isActive() {
        return active;
    }

    public BoardingStatus getBoardingStatus() {
        return boardingStatus;
    }

    public boolean isHasPhoto() {
        return hasPhoto;
    }
}
