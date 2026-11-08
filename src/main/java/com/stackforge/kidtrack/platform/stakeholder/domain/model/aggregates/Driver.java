package com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates;

import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.FullName;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A driver of the transport company. */
@Entity
@Table(name = "drivers")
public class Driver extends AuditableEntity {

    private static final int PHONE_MAX_LENGTH = 20;
    private static final int LICENSE_MAX_LENGTH = 50;

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /** The sign in account of the driver; routes and trips refer to the driver through it. */
    @Column(name = "user_id")
    private UUID userId;

    @Embedded
    private FullName fullName;

    @Column(length = PHONE_MAX_LENGTH)
    private String phone;

    @Column(name = "license_number", length = LICENSE_MAX_LENGTH)
    private String licenseNumber;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(nullable = false)
    private boolean active;

    protected Driver() {
    }

    public Driver(UUID organizationId, UUID userId, FullName fullName, String phone, String licenseNumber,
                  UUID vehicleId, boolean active) {
        this(UUID.randomUUID(), organizationId, userId, fullName, phone, licenseNumber, vehicleId, active);
    }

    /** Creates a driver with a predefined identifier (used by the demo data seeder). */
    public Driver(UUID id, UUID organizationId, UUID userId, FullName fullName, String phone, String licenseNumber,
                  UUID vehicleId, boolean active) {
        this.id = id;
        this.organizationId = organizationId;
        this.userId = userId;
        this.fullName = fullName;
        this.phone = normalizePhone(phone);
        this.licenseNumber = normalizeLicense(licenseNumber);
        this.vehicleId = vehicleId;
        this.active = active;
    }

    public void update(UUID userId, FullName fullName, String phone, String licenseNumber, UUID vehicleId,
                       boolean active) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = normalizePhone(phone);
        this.licenseNumber = normalizeLicense(licenseNumber);
        this.vehicleId = vehicleId;
        this.active = active;
    }

    public void assignVehicle(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    private static String normalizePhone(String phone) {
        var value = phone == null ? "" : phone.trim();
        if (value.length() > PHONE_MAX_LENGTH) {
            throw new IllegalArgumentException("The phone number is too long.");
        }
        return value;
    }

    private static String normalizeLicense(String license) {
        var value = license == null ? "" : license.trim().toUpperCase(java.util.Locale.ROOT);
        if (value.length() > LICENSE_MAX_LENGTH) {
            throw new IllegalArgumentException("The license number is too long.");
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

    public String getPhone() {
        return phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public boolean isActive() {
        return active;
    }
}
