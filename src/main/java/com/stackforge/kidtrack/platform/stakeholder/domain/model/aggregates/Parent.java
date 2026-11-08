package com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates;

import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Email;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.FullName;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A parent (guardian) of one or more students. */
@Entity
@Table(name = "parents")
public class Parent extends AuditableEntity {

    private static final int PHONE_MAX_LENGTH = 20;

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /** The sign in account of the parent, when it has one. */
    @Column(name = "user_id")
    private UUID userId;

    @Embedded
    private FullName fullName;

    @Column(name = "contact_email", length = 255)
    private String email;

    @Column(length = PHONE_MAX_LENGTH)
    private String phone;

    @Column(nullable = false)
    private boolean active;

    protected Parent() {
    }

    public Parent(UUID organizationId, UUID userId, FullName fullName, String email, String phone, boolean active) {
        this(UUID.randomUUID(), organizationId, userId, fullName, email, phone, active);
    }

    /** Creates a parent with a predefined identifier (used by the demo data seeder). */
    public Parent(UUID id, UUID organizationId, UUID userId, FullName fullName, String email, String phone,
                  boolean active) {
        this.id = id;
        this.organizationId = organizationId;
        this.userId = userId;
        this.fullName = fullName;
        this.email = normalizeEmail(email);
        this.phone = normalizePhone(phone);
        this.active = active;
    }

    public void update(FullName fullName, String email, String phone, boolean active) {
        this.fullName = fullName;
        this.email = normalizeEmail(email);
        this.phone = normalizePhone(phone);
        this.active = active;
    }

    public void linkUser(UUID userId) {
        this.userId = userId;
    }

    private static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? "" : new Email(email).value();
    }

    private static String normalizePhone(String phone) {
        var value = phone == null ? "" : phone.trim();
        if (value.length() > PHONE_MAX_LENGTH) {
            throw new IllegalArgumentException("The phone number is too long.");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public FullName getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isActive() {
        return active;
    }
}
