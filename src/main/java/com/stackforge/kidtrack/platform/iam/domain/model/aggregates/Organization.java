package com.stackforge.kidtrack.platform.iam.domain.model.aggregates;

import com.stackforge.kidtrack.platform.iam.domain.model.valueobjects.OrganizationStatus;
import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A transport company (tenant) that owns the routes, vehicles, drivers and families of the platform. */
@Entity
@Table(name = "organizations")
public class Organization extends AuditableEntity {

    public static final int NAME_MAX_LENGTH = 150;

    @Id
    private UUID id;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationStatus status;

    protected Organization() {
    }

    public Organization(String name, OrganizationStatus status) {
        this(UUID.randomUUID(), name, status);
    }

    /** Creates an organization with a predefined identifier (used by the demo data seeder). */
    public Organization(UUID id, String name, OrganizationStatus status) {
        this.id = id;
        this.name = validateName(name);
        this.status = status == null ? OrganizationStatus.ACTIVE : status;
    }

    public void rename(String name) {
        this.name = validateName(name);
    }

    public void suspend() {
        this.status = OrganizationStatus.SUSPENDED;
    }

    public void activate() {
        this.status = OrganizationStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == OrganizationStatus.ACTIVE;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Organization name cannot be empty.");
        }
        var value = name.trim();
        if (value.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Organization name is too long.");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public OrganizationStatus getStatus() {
        return status;
    }
}
