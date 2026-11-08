package com.stackforge.kidtrack.platform.trip.interfaces.rest;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Trip;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.CreateTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.DeleteTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.PatchTripCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetTripByIdQuery;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetTripsQuery;
import com.stackforge.kidtrack.platform.trip.domain.services.TripCommandService;
import com.stackforge.kidtrack.platform.trip.domain.services.TripQueryService;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.resources.TripResources.TripRequestResource;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.resources.TripResources.TripResource;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.transform.TripResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trips", description = "Runs of a route on a given day, executed and monitored in real time")
public class TripsController {

    /** The fields of a trip a client can change with a PATCH; anything else the web application sends is ignored. */
    private static final Set<String> PATCHABLE_FIELDS =
            Set.of("status", "startTime", "endTime", "studentsBoarded", "currentStop", "currentLocation");

    private final TripCommandService commandService;
    private final TripQueryService queryService;

    public TripsController(TripCommandService commandService, TripQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the trips", description = "Trips of the organization, newest first. "
            + "Optional filters: routeId, driverId, status and scheduledDate.")
    public List<TripResource> getTrips(@RequestParam(required = false) String organizationId,
                                       @RequestParam(required = false) String routeId,
                                       @RequestParam(required = false) String driverId,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String scheduledDate,
                                       @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        return queryService.handle(new GetTripsQuery(scope, Ids.parseOrNull(routeId), Ids.parseOrNull(driverId),
                status, scheduledDate)).stream().map(TripResourceAssembler::toResource).toList();
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get a trip by id")
    public TripResource getTripById(@PathVariable UUID tripId, @AuthenticationPrincipal Jwt jwt) {
        return TripResourceAssembler.toResource(findInScope(tripId, jwt));
    }

    @PostMapping
    @Operation(summary = "Schedule a trip",
            description = "Creates a SCHEDULED trip of a route; driver, vehicle and students come from the route.")
    public ResponseEntity<TripResource> createTrip(@Valid @RequestBody TripRequestResource resource,
                                                   @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var organizationId = Caller.requireScope(caller, Ids.parseOrNull(resource.organizationId()));
        var trip = commandService.handle(new CreateTripCommand(organizationId,
                Ids.parse(resource.routeId(), "routeId"), resource.scheduledDate(), resource.scheduledStartTime()));
        return ResponseEntity.status(HttpStatus.CREATED).body(TripResourceAssembler.toResource(trip));
    }

    @PatchMapping("/{tripId}")
    @Operation(summary = "Update a trip",
            description = "Starts, completes, cancels or restarts the trip and records its progress. "
                    + "Administrators, and the driver assigned to the trip.")
    public TripResource patchTrip(@PathVariable UUID tripId, @RequestBody Map<String, Object> body,
                                  @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAnyRole(caller, "ADMIN", "DRIVER");
        var trip = findInScope(tripId, jwt);
        if (!caller.isAdmin() && !caller.userId().equals(trip.getDriverId())) {
            throw new AccessDeniedException("Only the driver assigned to the trip can update it.");
        }
        var changes = new HashMap<String, Object>();
        body.forEach((field, value) -> {
            if (PATCHABLE_FIELDS.contains(field)) {
                changes.put(field, value);
            }
        });
        return TripResourceAssembler.toResource(commandService.handle(new PatchTripCommand(tripId, changes)));
    }

    @DeleteMapping("/{tripId}")
    @Operation(summary = "Delete a trip")
    public ResponseEntity<Void> deleteTrip(@PathVariable UUID tripId, @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(tripId, jwt);
        commandService.handle(new DeleteTripCommand(tripId));
        return ResponseEntity.noContent().build();
    }

    private Trip findInScope(UUID tripId, Jwt jwt) {
        var trip = queryService.handle(new GetTripByIdQuery(tripId))
                .orElseThrow(() -> new NotFoundException("Trip was not found."));
        var caller = Caller.from(jwt);
        if (caller != null && caller.organizationId() != null
                && !caller.organizationId().equals(trip.getOrganizationId())) {
            throw new AccessDeniedException("The trip belongs to another organization.");
        }
        return trip;
    }
}
