package com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Handover;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.HandoverOutcome;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HandoverRepository extends JpaRepository<Handover, UUID> {

    List<Handover> findByOrganizationId(UUID organizationId);

    List<Handover> findByTripId(UUID tripId);

    boolean existsByTripIdAndChildIdAndOutcome(UUID tripId, UUID childId, HandoverOutcome outcome);
}
