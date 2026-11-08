package com.stackforge.kidtrack.platform.trip.application.internal.queryservices;

import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Incident;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Trip;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetIncidentByIdQuery;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetIncidentsQuery;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetTripByIdQuery;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetTripsQuery;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentStatus;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.TripStatus;
import com.stackforge.kidtrack.platform.trip.domain.services.TripQueryService;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.TripRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TripQueryServiceImpl implements TripQueryService {

    private final TripRepository tripRepository;
    private final IncidentRepository incidentRepository;

    public TripQueryServiceImpl(TripRepository tripRepository, IncidentRepository incidentRepository) {
        this.tripRepository = tripRepository;
        this.incidentRepository = incidentRepository;
    }

    @Override
    public List<Trip> handle(GetTripsQuery query) {
        var trips = query.organizationId() == null
                ? tripRepository.findAll() : tripRepository.findByOrganizationId(query.organizationId());
        var status = query.status() == null || query.status().isBlank() ? null : TripStatus.from(query.status());
        return trips.stream()
                .filter(trip -> query.routeId() == null || trip.getRouteId().equals(query.routeId()))
                .filter(trip -> query.driverId() == null || Objects.equals(trip.getDriverId(), query.driverId()))
                .filter(trip -> status == null || trip.getStatus() == status)
                .filter(trip -> query.scheduledDate() == null || query.scheduledDate().isBlank()
                        || trip.getScheduledDate().equals(query.scheduledDate().trim()))
                .sorted(Comparator.comparing(Trip::getScheduledDate)
                        .thenComparing(Trip::getScheduledStartTime).reversed())
                .toList();
    }

    @Override
    public Optional<Trip> handle(GetTripByIdQuery query) {
        return tripRepository.findById(query.tripId());
    }

    @Override
    public List<Incident> handle(GetIncidentsQuery query) {
        var incidents = query.organizationId() == null
                ? incidentRepository.findAll() : incidentRepository.findByOrganizationId(query.organizationId());
        var status = query.status() == null || query.status().isBlank() ? null : IncidentStatus.from(query.status());
        return incidents.stream()
                .filter(incident -> query.tripId() == null || query.tripId().equals(incident.getTripId()))
                .filter(incident -> status == null || incident.getStatus() == status)
                .sorted(Comparator.comparing(Incident::getReportedAt).reversed())
                .toList();
    }

    @Override
    public Optional<Incident> handle(GetIncidentByIdQuery query) {
        return incidentRepository.findById(query.incidentId());
    }
}
