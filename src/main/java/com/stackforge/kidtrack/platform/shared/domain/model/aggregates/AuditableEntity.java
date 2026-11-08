package com.stackforge.kidtrack.platform.shared.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Base class for entities that carry audit timestamps managed by the persistence layer.
 * Equivalent to the {@code IAuditableEntity} of the original platform.
 */
@MappedSuperclass
public abstract class AuditableEntity {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
