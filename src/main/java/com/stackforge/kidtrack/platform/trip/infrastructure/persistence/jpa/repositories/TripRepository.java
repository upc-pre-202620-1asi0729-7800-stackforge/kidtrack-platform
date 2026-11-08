package com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories;

import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Trip;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.TripStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByOrganizationId(UUID organizationId);

    List<Trip> findByRouteId(UUID routeId);

    boolean existsByRouteIdAndScheduledDateAndScheduledStartTimeAndStatusNot(UUID routeId, String scheduledDate,
                                                                            String scheduledStartTime,
                                                                            TripStatus status);
}
