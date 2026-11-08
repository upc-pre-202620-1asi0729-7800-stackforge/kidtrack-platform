package com.stackforge.kidtrack.platform.iam.domain.model.aggregates;

import com.stackforge.kidtrack.platform.iam.domain.model.valueobjects.RoleTier;
import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Email;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.FullName;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import jakarta.persistence.Entity;

/** A person that can sign in to the platform (administrator, driver or parent). */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User extends AuditableEntity {

    private static final int PHONE_MAX_LENGTH = 20;

    @Id
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Embedded
    private FullName fullName;

    @Embedded
    private Email email;

    @Column(length = PHONE_MAX_LENGTH)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleTier role;

    protected User() {
    }

    public User(UUID organizationId, FullName fullName, Email email, String phone, String passwordHash, RoleTier role) {
        this(UUID.randomUUID(), organizationId, fullName, email, phone, passwordHash, role);
    }

    /** Creates a user with a predefined identifier (used by the demo data seeder). */
    public User(UUID id, UUID organizationId, FullName fullName, Email email, String phone, String passwordHash,
                RoleTier role) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be empty.");
        }
        this.id = id;
        this.organizationId = organizationId;
        this.fullName = fullName;
        this.email = email;
        this.phone = normalizePhone(phone);
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public void updateProfile(FullName fullName, Email email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = normalizePhone(phone);
    }

    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be empty.");
        }
        this.passwordHash = newPasswordHash;
    }

    public void changeRole(RoleTier role) {
        this.role = role;
    }

    public void assignOrganization(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public boolean isAdmin() {
        return role == RoleTier.ADMIN;
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        var value = phone.trim();
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

    public FullName getFullName() {
        return fullName;
    }

    public Email getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public RoleTier getRole() {
        return role;
    }
}
