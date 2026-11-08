package com.stackforge.kidtrack.platform.trip.application.internal.eventhandlers;

import com.stackforge.kidtrack.platform.fleet.domain.model.events.RouteEvents.RouteConfigurationChanged;
import com.stackforge.kidtrack.platform.fleet.domain.model.events.RouteEvents.RouteDeleted;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Trip;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.TripDeleted;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.TripRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the trips consistent with the Fleet context. The handlers run synchronously, inside the transaction of the
 * Fleet command that published the event.
 */
@Component
public class RouteEventHandler {

    private final TripRepository tripRepository;
    private final IncidentRepository incidentRepository;
    private final ApplicationEventPublisher events;

    public RouteEventHandler(TripRepository tripRepository, IncidentRepository incidentRepository,
                             ApplicationEventPublisher events) {
        this.tripRepository = tripRepository;
        this.incidentRepository = incidentRepository;
        this.events = events;
    }

    /** The trips that are still open follow the new driver, vehicle and students of the route. */
    @EventListener
    public void on(RouteConfigurationChanged event) {
        tripRepository.findByRouteId(event.routeId()).stream()
                .filter(Trip::isOpen)
                .forEach(trip -> trip.followRoute(event.driverId(), event.driverName(), event.vehicleId(),
                        event.vehiclePlate(), event.studentIds()));
    }

    /** Deleting a route deletes its trips and incidents. */
    @EventListener
    public void on(RouteDeleted event) {
        var trips = tripRepository.findByRouteId(event.routeId());
        incidentRepository.deleteAll(incidentRepository.findByRouteId(event.routeId()));
        trips.forEach(trip -> events.publishEvent(new TripDeleted(trip.getId())));
        tripRepository.deleteAll(trips);
    }
}
