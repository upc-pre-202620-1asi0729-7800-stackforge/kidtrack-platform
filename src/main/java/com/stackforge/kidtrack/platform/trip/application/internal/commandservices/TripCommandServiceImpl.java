package com.stackforge.kidtrack.platform.trip.application.internal.commandservices;

import com.stackforge.kidtrack.platform.fleet.interfaces.acl.FleetContextFacade;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.BusinessRuleException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.ConflictException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Dates;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Incident;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Trip;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.CreateTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.DeleteTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.PatchIncidentCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.PatchTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.ReportIncidentCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.TripDeleted;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentSeverity;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentStatus;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentType;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.TripStatus;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.TripType;
import com.stackforge.kidtrack.platform.trip.domain.services.TripCommandService;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.TripRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TripCommandServiceImpl implements TripCommandService {

    private final TripRepository tripRepository;
    private final IncidentRepository incidentRepository;
    private final FleetContextFacade fleetFacade;
    private final ApplicationEventPublisher events;

    public TripCommandServiceImpl(TripRepository tripRepository, IncidentRepository incidentRepository,
                                  FleetContextFacade fleetFacade, ApplicationEventPublisher events) {
        this.tripRepository = tripRepository;
        this.incidentRepository = incidentRepository;
        this.fleetFacade = fleetFacade;
        this.events = events;
    }

    @Override
    public Trip handle(CreateTripCommand command) {
        var route = fleetFacade.findRoute(command.routeId())
                .orElseThrow(() -> new NotFoundException("Route was not found."));
        if (!route.organizationId().equals(command.organizationId())) {
            throw new BusinessRuleException("The route belongs to another organization.");
        }
        if ("INACTIVE".equals(route.status())) {
            throw new BusinessRuleException("A trip cannot be scheduled on an inactive route.");
        }
        if (route.driverId() == null) {
            throw new BusinessRuleException("The route has no driver assigned.");
        }
        var date = resolveDate(command.scheduledDate());
        var time = command.scheduledStartTime() == null || command.scheduledStartTime().isBlank()
                ? route.scheduledStartTime() : command.scheduledStartTime().trim();
        if (tripRepository.existsByRouteIdAndScheduledDateAndScheduledStartTimeAndStatusNot(route.id(), date, time,
                TripStatus.CANCELLED)) {
            throw new ConflictException("The route already has a trip on " + date + " at " + time + ".");
        }
        var trip = new Trip(command.organizationId(), route.id(), route.name(), route.driverId(), route.driverName(),
                route.vehicleId(), route.vehiclePlate(), route.studentIds(), TripType.from(route.type()), date, time);
        return tripRepository.save(trip);
    }

    @Override
    public Trip handle(PatchTripCommand command) {
        var trip = tripRepository.findById(command.tripId())
                .orElseThrow(() -> new NotFoundException("Trip was not found."));
        var changes = command.changes();
        var stop = changes.get("currentStop") == null ? null : String.valueOf(changes.get("currentStop"));
        var location = changes.get("currentLocation") == null ? null : String.valueOf(changes.get("currentLocation"));

        if (changes.get("status") != null) {
            var target = TripStatus.from(String.valueOf(changes.get("status")));
            if (target != trip.getStatus()) {
                switch (target) {
                    case EN_ROUTE -> trip.start(instantOf(changes.get("startTime")), stop);
                    case COMPLETED -> trip.complete(instantOf(changes.get("endTime")));
                    case CANCELLED -> trip.cancel();
                    case SCHEDULED -> trip.reset();
                    default -> throw new BusinessRuleException("The trip status change is not allowed.");
                }
            }
        }
        trip.updateProgress(intOf(changes.get("studentsBoarded")), stop, location,
                changes.containsKey("currentStop"), changes.containsKey("currentLocation"));
        return tripRepository.save(trip);
    }

    /** Deleting a trip also deletes its incidents; the notifications are removed by the Notifications context. */
    @Override
    public void handle(DeleteTripCommand command) {
        var trip = tripRepository.findById(command.tripId())
                .orElseThrow(() -> new NotFoundException("Trip was not found."));
        incidentRepository.deleteAll(incidentRepository.findByTripId(trip.getId()));
        events.publishEvent(new TripDeleted(trip.getId()));
        tripRepository.delete(trip);
    }

    @Override
    public Incident handle(ReportIncidentCommand command) {
        var routeId = command.routeId();
        if (command.tripId() != null) {
            var trip = tripRepository.findById(command.tripId())
                    .orElseThrow(() -> new NotFoundException("Trip was not found."));
            if (!trip.getOrganizationId().equals(command.organizationId())) {
                throw new BusinessRuleException("The trip belongs to another organization.");
            }
            routeId = routeId != null ? routeId : trip.getRouteId();
        }
        var routeName = routeId == null ? "" : fleetFacade.findRoute(routeId)
                .map(FleetContextFacade.RouteSummary::name).orElse("");
        var incident = new Incident(command.organizationId(), command.tripId(), routeId, routeName,
                IncidentType.from(command.type()), IncidentSeverity.from(command.severity()), command.description(),
                command.reportedBy(), Dates.parseInstant(command.timestamp()));
        return incidentRepository.save(incident);
    }

    @Override
    public Incident handle(PatchIncidentCommand command) {
        var incident = incidentRepository.findById(command.incidentId())
                .orElseThrow(() -> new NotFoundException("Incident was not found."));
        if (command.status() != null) {
            if (IncidentStatus.from(command.status()) == IncidentStatus.RESOLVED) {
                incident.resolve();
            } else {
                incident.reopen();
            }
        }
        return incidentRepository.save(incident);
    }

    private static String resolveDate(String scheduledDate) {
        if (scheduledDate == null || scheduledDate.isBlank()) {
            return LocalDate.now().toString();
        }
        try {
            return LocalDate.parse(scheduledDate.trim()).toString();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("The date of the trip must be an ISO date (yyyy-MM-dd).");
        }
    }

    private static java.time.Instant instantOf(Object value) {
        return value == null ? null : Dates.parseInstant(String.valueOf(value));
    }

    private static Integer intOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("studentsBoarded must be a number.");
        }
    }
}
