package com.stackforge.kidtrack.platform.subscription.domain.model.aggregates;

import com.stackforge.kidtrack.platform.shared.domain.model.aggregates.AuditableEntity;
import com.stackforge.kidtrack.platform.subscription.domain.model.valueobjects.PlanTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** A commercial plan that limits how many routes, drivers and students an organization can manage. */
@Entity
@Table(name = "plans")
public class Plan extends AuditableEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private PlanTier tier;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "max_routes", nullable = false)
    private int maxRoutes;

    @Column(name = "max_drivers", nullable = false)
    private int maxDrivers;

    @Column(name = "max_students", nullable = false)
    private int maxStudents;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String description;

    protected Plan() {
    }

    public Plan(PlanTier tier, String name, int maxRoutes, int maxDrivers, int maxStudents, BigDecimal price,
                String description) {
        this(UUID.randomUUID(), tier, name, maxRoutes, maxDrivers, maxStudents, price, description);
    }

    /** Creates a plan with a predefined identifier (used by the demo data seeder). */
    public Plan(UUID id, PlanTier tier, String name, int maxRoutes, int maxDrivers, int maxStudents,
                BigDecimal price, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Plan name cannot be empty.");
        }
        if (maxRoutes < 0 || maxDrivers < 0 || maxStudents < 0) {
            throw new IllegalArgumentException("Plan quotas cannot be negative.");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Plan price cannot be negative.");
        }
        this.id = id;
        this.tier = tier;
        this.name = name.trim();
        this.maxRoutes = maxRoutes;
        this.maxDrivers = maxDrivers;
        this.maxStudents = maxStudents;
        this.price = price;
        this.description = description == null ? "" : description.trim();
    }

    public boolean isWithinRouteQuota(long current) {
        return current <= maxRoutes;
    }

    public boolean isWithinDriverQuota(long current) {
        return current <= maxDrivers;
    }

    public boolean isWithinStudentQuota(long current) {
        return current <= maxStudents;
    }

    public UUID getId() {
        return id;
    }

    public PlanTier getTier() {
        return tier;
    }

    public String getName() {
        return name;
    }

    public int getMaxRoutes() {
        return maxRoutes;
    }

    public int getMaxDrivers() {
        return maxDrivers;
    }

    public int getMaxStudents() {
        return maxStudents;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }
}
