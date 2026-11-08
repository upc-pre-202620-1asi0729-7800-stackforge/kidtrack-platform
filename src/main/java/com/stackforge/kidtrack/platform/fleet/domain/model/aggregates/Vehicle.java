package com.stackforge.kidtrack.platform.fleet.domain.model.aggregates;

import com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects.VehicleStatus;
import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/** A vehicle of the fleet of an organization. */
@Entity
@Table(name = "vehicles", uniqueConstraints = @UniqueConstraint(name = "uk_vehicles_org_plate",
        columnNames = {"organization_id", "plate"}))
public class Vehicle extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 20)
    private String plate;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus status;

    protected Vehicle() {
    }

    public Vehicle(UUID organizationId, String plate, String model, int capacity, VehicleStatus status) {
        this(UUID.randomUUID(), organizationId, plate, model, capacity, status);
    }

    /** Creates a vehicle with a predefined identifier (used by the demo data seeder). */
    public Vehicle(UUID id, UUID organizationId, String plate, String model, int capacity, VehicleStatus status) {
        this.id = id;
        this.organizationId = organizationId;
        this.status = status == null ? VehicleStatus.ACTIVE : status;
        apply(plate, model, capacity);
    }

    public void update(String plate, String model, int capacity, VehicleStatus status) {
        apply(plate, model, capacity);
        this.status = status;
    }

    private void apply(String plate, String model, int capacity) {
        if (plate == null || plate.isBlank()) {
            throw new IllegalArgumentException("The plate of the vehicle cannot be empty.");
        }
        if (plate.trim().length() > 20) {
            throw new IllegalArgumentException("The plate of the vehicle is too long.");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("The model of the vehicle cannot be empty.");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Vehicle capacity must be positive.");
        }
        this.plate = plate.trim().toUpperCase(java.util.Locale.ROOT);
        this.model = model.trim();
        this.capacity = capacity;
    }

    public boolean isAvailable() {
        return status == VehicleStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getPlate() {
        return plate;
    }

    public String getModel() {
        return model;
    }

    public int getCapacity() {
        return capacity;
    }

    public VehicleStatus getStatus() {
        return status;
    }
}
