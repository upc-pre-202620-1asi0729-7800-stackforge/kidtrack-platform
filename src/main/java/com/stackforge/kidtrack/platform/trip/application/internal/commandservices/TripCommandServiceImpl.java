package com.stackforge.kidtrack.platform.trip.application.internal.commandservices;

import com.stackforge.kidtrack.platform.fleet.interfaces.acl.FleetContextFacade;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.BusinessRuleException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.ConflictException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Dates;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.acl.StakeholderContextFacade;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Handover;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Incident;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Trip;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.AddIncidentUpdateCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.CreateTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.DeleteTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.PatchIncidentCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.PatchTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.RecordHandoverCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.RecordServiceCallCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.ReportIncidentCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.EmergencyNotice;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.EmergencyStage;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.StudentHandedOver;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.StudentNotDelivered;
import com.stackforge.kidtrack.platform.trip.domain.model.events.TripEvents.TripDeleted;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.EmergencyType;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.HandoverOutcome;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentSeverity;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentStatus;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.IncidentType;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.TripStatus;
import com.stackforge.kidtrack.platform.trip.domain.model.valueobjects.TripType;
import com.stackforge.kidtrack.platform.trip.domain.services.TripCommandService;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.HandoverRepository;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.stackforge.kidtrack.platform.trip.infrastructure.persistence.jpa.repositories.TripRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
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

    private final HandoverRepository handoverRepository;
    private final StakeholderContextFacade stakeholderFacade;

    public TripCommandServiceImpl(TripRepository tripRepository, IncidentRepository incidentRepository,
                                  FleetContextFacade fleetFacade, ApplicationEventPublisher events,
                                  HandoverRepository handoverRepository, StakeholderContextFacade stakeholderFacade) {
        this.handoverRepository = handoverRepository;
        this.stakeholderFacade = stakeholderFacade;
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

    /** Deleting a trip also deletes its incidents and handovers; the notifications are removed by the Notifications context. */
    @Override
    public void handle(DeleteTripCommand command) {
        var trip = tripRepository.findById(command.tripId())
                .orElseThrow(() -> new NotFoundException("Trip was not found."));
        incidentRepository.deleteAll(incidentRepository.findByTripId(trip.getId()));
        handoverRepository.deleteAll(handoverRepository.findByTripId(trip.getId()));
        events.publishEvent(new TripDeleted(trip.getId()));
        tripRepository.delete(trip);
    }

    @Override
    public Incident handle(ReportIncidentCommand command) {
        var emergencyType = EmergencyType.fromNullable(command.emergencyType());
        var emergencyTrip = emergencyType == null ? null : tripForEmergency(command.tripId());
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
        if (emergencyTrip == null) {
            return incidentRepository.save(incident);
        }
        incident.declareEmergency(emergencyType, command.location(), command.studentsSafe(),
                studentsAboard(emergencyTrip, command.studentIds()));
        var saved = incidentRepository.save(incident);
        notifyParents(EmergencyStage.DECLARED, saved, emergencyTrip, "");
        return saved;
    }

    @Override
    public Incident handle(PatchIncidentCommand command) {
        var incident = findIncident(command.incidentId());
        var closedNow = false;
        if (command.status() != null) {
            if (IncidentStatus.from(command.status()) != IncidentStatus.RESOLVED) {
                incident.reopen();
            } else if (incident.getStatus() == IncidentStatus.OPEN) {
                incident.resolve(command.resolution());
                closedNow = true;
            }
        }
        var saved = incidentRepository.save(incident);
        if (closedNow && saved.isEmergency()) {
            notifyParents(EmergencyStage.RESOLVED, saved, tripOf(saved), saved.getResolution());
        }
        return saved;
    }

    @Override
    public Incident handle(RecordServiceCallCommand command) {
        var incident = findIncident(command.incidentId());
        incident.recordServiceCall(command.contactedBy());
        var saved = incidentRepository.save(incident);
        notifyParents(EmergencyStage.SERVICE_CONTACTED, saved, tripOf(saved), "");
        return saved;
    }

    @Override
    public Incident handle(AddIncidentUpdateCommand command) {
        var incident = findIncident(command.incidentId());
        var update = incident.addUpdate(command.message(), command.studentsSafe(), command.author());
        var saved = incidentRepository.save(incident);
        notifyParents(EmergencyStage.UPDATED, saved, tripOf(saved), update.message());
        return saved;
    }

    /**
     * Records who received a student. The student is only handed over to a person registered for them; otherwise
     * the attempt is recorded as not delivered, the parent is informed and nothing else happens.
     */
    @Override
    public Handover handle(RecordHandoverCommand command) {
        var trip = tripRepository.findById(command.tripId())
                .orElseThrow(() -> new NotFoundException("Trip was not found."));
        if (trip.getStatus() != TripStatus.EN_ROUTE) {
            throw new BusinessRuleException("A student can only be handed over while the trip is en route.");
        }
        if (!trip.getStudentIds().contains(command.childId().toString())) {
            throw new BusinessRuleException("The student does not belong to this trip.");
        }
        if (handoverRepository.existsByTripIdAndChildIdAndOutcome(trip.getId(), command.childId(),
                HandoverOutcome.DELIVERED)) {
            throw new ConflictException("The student was already handed over on this trip.");
        }
        var child = stakeholderFacade.childContact(command.childId())
                .orElseThrow(() -> new NotFoundException("Student was not found."));
        if (command.authorizedPersonId() == null) {
            var saved = handoverRepository.save(Handover.notDelivered(trip.getOrganizationId(), trip.getId(),
                    child.childId(), child.name(), command.receiverName(), command.recordedBy()));
            events.publishEvent(new StudentNotDelivered(trip.getOrganizationId(), trip.getId(), child.childId(),
                    trip.getDriverName(), trip.getVehiclePlate()));
            return saved;
        }
        var receiver = stakeholderFacade.authorizedPerson(command.authorizedPersonId())
                .filter(person -> person.childId().equals(command.childId()))
                .orElseThrow(() -> new BusinessRuleException("That person is not registered to receive the student."));
        var saved = handoverRepository.save(Handover.delivered(trip.getOrganizationId(), trip.getId(),
                child.childId(), child.name(), receiver.id(), receiver.fullName(), receiver.relationship(),
                command.recordedBy()));
        events.publishEvent(new StudentHandedOver(trip.getOrganizationId(), trip.getId(), child.childId(),
                receiver.fullName(), receiver.relationship()));
        return saved;
    }

    /** An emergency belongs to a trip in progress, and a trip has at most one open emergency. */
    private Trip tripForEmergency(UUID tripId) {
        if (tripId == null) {
            throw new BusinessRuleException("An emergency must belong to a trip.");
        }
        var trip = tripRepository.findById(tripId).orElseThrow(() -> new NotFoundException("Trip was not found."));
        if (trip.getStatus() != TripStatus.EN_ROUTE) {
            throw new BusinessRuleException("An emergency can only be declared while the trip is en route.");
        }
        if (incidentRepository.findByTripId(tripId).stream()
                .anyMatch(other -> other.isEmergency() && other.getStatus() == IncidentStatus.OPEN)) {
            throw new ConflictException("The trip already has an open emergency.");
        }
        return trip;
    }

    /** The students named by the driver as aboard; every student of the trip when none are named. */
    private static List<String> studentsAboard(Trip trip, List<String> named) {
        if (named == null || named.isEmpty()) {
            return List.copyOf(trip.getStudentIds());
        }
        if (!trip.getStudentIds().containsAll(named)) {
            throw new BusinessRuleException("A student named as aboard does not belong to this trip.");
        }
        return named.stream().distinct().toList();
    }

    private Incident findIncident(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new NotFoundException("Incident was not found."));
    }

    private Trip tripOf(Incident incident) {
        return tripRepository.findById(incident.getTripId())
                .orElseThrow(() -> new NotFoundException("Trip was not found."));
    }

    private void notifyParents(EmergencyStage stage, Incident incident, Trip trip, String detail) {
        var type = incident.getEmergencyType();
        events.publishEvent(new EmergencyNotice(stage, incident.getOrganizationId(), trip.getId(),
                List.copyOf(incident.getStudentIds()), type.label(), type.serviceName(), type.servicePhone(),
                trip.getDriverName(), trip.getVehiclePlate(), incident.getLocation(), incident.getStudentsSafe(),
                detail));
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
