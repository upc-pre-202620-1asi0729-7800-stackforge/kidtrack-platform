package com.stackforge.kidtrack.platform.fleet.application.internal.commandservices;

import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Route;
import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Vehicle;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.CreateRouteCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.CreateVehicleCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.DeleteRouteCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.DeleteVehicleCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.UpdateRouteCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.UpdateVehicleCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.WaypointData;
import com.stackforge.kidtrack.platform.fleet.domain.model.entities.Stop;
import com.stackforge.kidtrack.platform.fleet.domain.model.events.RouteEvents.RouteConfigurationChanged;
import com.stackforge.kidtrack.platform.fleet.domain.model.events.RouteEvents.RouteDeleted;
import com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects.RouteStatus;
import com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects.RouteType;
import com.stackforge.kidtrack.platform.fleet.domain.model.valueobjects.VehicleStatus;
import com.stackforge.kidtrack.platform.fleet.domain.services.FleetCommandService;
import com.stackforge.kidtrack.platform.fleet.infrastructure.persistence.jpa.repositories.RouteRepository;
import com.stackforge.kidtrack.platform.fleet.infrastructure.persistence.jpa.repositories.VehicleRepository;
import com.stackforge.kidtrack.platform.iam.interfaces.acl.IamContextFacade;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.BusinessRuleException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.ConflictException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Coordinates;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.acl.StakeholderContextFacade;
import com.stackforge.kidtrack.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FleetCommandServiceImpl implements FleetCommandService {

    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final StakeholderContextFacade stakeholderFacade;
    private final IamContextFacade iamFacade;
    private final SubscriptionContextFacade subscriptionFacade;
    private final ApplicationEventPublisher events;

    public FleetCommandServiceImpl(VehicleRepository vehicleRepository, RouteRepository routeRepository,
                                   StakeholderContextFacade stakeholderFacade, IamContextFacade iamFacade,
                                   SubscriptionContextFacade subscriptionFacade, ApplicationEventPublisher events) {
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.stakeholderFacade = stakeholderFacade;
        this.iamFacade = iamFacade;
        this.subscriptionFacade = subscriptionFacade;
        this.events = events;
    }

    // ---- Vehicles ----------------------------------------------------------------------------------------

    @Override
    public Vehicle handle(CreateVehicleCommand command) {
        var plate = normalizePlate(command.plate());
        if (vehicleRepository.existsByOrganizationIdAndPlate(command.organizationId(), plate)) {
            throw new ConflictException("A vehicle with the plate " + plate + " already exists.");
        }
        return vehicleRepository.save(new Vehicle(command.organizationId(), plate, command.model(),
                command.capacity(), VehicleStatus.from(command.status())));
    }

    @Override
    public Vehicle handle(UpdateVehicleCommand command) {
        var vehicle = findVehicle(command.vehicleId());
        var plate = command.plate() != null ? normalizePlate(command.plate()) : vehicle.getPlate();
        if (!plate.equals(vehicle.getPlate())
                && vehicleRepository.existsByOrganizationIdAndPlate(vehicle.getOrganizationId(), plate)) {
            throw new ConflictException("A vehicle with the plate " + plate + " already exists.");
        }
        vehicle.update(plate,
                command.model() != null ? command.model() : vehicle.getModel(),
                command.capacity() != null ? command.capacity() : vehicle.getCapacity(),
                command.status() != null ? VehicleStatus.from(command.status()) : vehicle.getStatus());
        var saved = vehicleRepository.save(vehicle);
        routeRepository.findByVehicleId(saved.getId()).forEach(route -> route.refreshVehiclePlate(saved.getPlate()));
        return saved;
    }

    /** Deleting a vehicle releases it from the routes that used it. */
    @Override
    public void handle(DeleteVehicleCommand command) {
        var vehicle = findVehicle(command.vehicleId());
        routeRepository.findByVehicleId(vehicle.getId()).forEach(Route::releaseVehicle);
        vehicleRepository.delete(vehicle);
    }

    // ---- Routes ------------------------------------------------------------------------------------------

    @Override
    public Route handle(CreateRouteCommand command) {
        subscriptionFacade.activePlanLimits(command.organizationId()).ifPresent(limits -> {
            if (routeRepository.countByOrganizationId(command.organizationId()) + 1 > limits.maxRoutes()) {
                throw new BusinessRuleException(
                        "The plan of the organization allows at most " + limits.maxRoutes() + " routes.");
            }
        });
        var route = new Route(command.organizationId(), command.name(), RouteType.from(command.type()));
        configure(route, command.name(), command.type(), command.driverId(), command.driverName(),
                command.vehicleId(), command.vehiclePlate(), command.studentIds(), command.scheduledStartTime(),
                command.status(), command.waypoints());
        return routeRepository.save(route);
    }

    @Override
    public Route handle(UpdateRouteCommand command) {
        var route = findRoute(command.routeId());
        configure(route, command.name(), command.type(), command.driverId(), command.driverName(),
                command.vehicleId(), command.vehiclePlate(), command.studentIds(), command.scheduledStartTime(),
                command.status(), command.waypoints());
        var saved = routeRepository.save(route);
        events.publishEvent(new RouteConfigurationChanged(saved.getId(), saved.getDriverId(), saved.getDriverName(),
                saved.getVehicleId(), saved.getVehiclePlate(), saved.getStudentIds()));
        return saved;
    }

    /** Deleting a route also deletes its trips (see the Trip context, which listens to the event). */
    @Override
    public void handle(DeleteRouteCommand command) {
        var route = findRoute(command.routeId());
        events.publishEvent(new RouteDeleted(route.getId()));
        routeRepository.delete(route);
    }

    private void configure(Route route, String name, String type, UUID driverId, String driverName, UUID vehicleId,
                           String vehiclePlate, List<String> studentIds, String scheduledStartTime, String status,
                           List<WaypointData> waypoints) {
        var plate = vehiclePlate;
        if (vehicleId != null) {
            var vehicle = findVehicle(vehicleId);
            if (!vehicle.getOrganizationId().equals(route.getOrganizationId())) {
                throw new BusinessRuleException("The vehicle belongs to another organization.");
            }
            plate = vehicle.getPlate();
        }
        var resolvedDriverName = driverName;
        if (driverId != null) {
            resolvedDriverName = stakeholderFacade.driverFullName(driverId)
                    .or(() -> iamFacade.fullNameOfUser(driverId))
                    .orElse(driverName);
        }
        route.replaceConfiguration(name, RouteType.from(type), RouteStatus.from(status), driverId,
                resolvedDriverName, vehicleId, plate, studentIds, scheduledStartTime, toStops(waypoints));
    }

    private static List<Stop> toStops(List<WaypointData> waypoints) {
        var stops = new ArrayList<Stop>();
        if (waypoints == null) {
            return stops;
        }
        for (var index = 0; index < waypoints.size(); index++) {
            var waypoint = waypoints.get(index);
            var position = waypoint.order() != null && waypoint.order() > 0 ? waypoint.order() : index + 1;
            var name = waypoint.name() == null || waypoint.name().isBlank() ? "Parada " + (index + 1) : waypoint.name();
            stops.add(new Stop(position, name, new Coordinates(waypoint.lat(), waypoint.lng()),
                    waypoint.studentIds()));
        }
        return stops;
    }

    private Vehicle findVehicle(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle was not found."));
    }

    private Route findRoute(UUID routeId) {
        return routeRepository.findById(routeId).orElseThrow(() -> new NotFoundException("Route was not found."));
    }

    private static String normalizePlate(String plate) {
        if (plate == null || plate.isBlank()) {
            throw new IllegalArgumentException("The plate of the vehicle cannot be empty.");
        }
        return plate.trim().toUpperCase(Locale.ROOT);
    }
}
