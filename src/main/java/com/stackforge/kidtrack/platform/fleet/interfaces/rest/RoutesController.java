package com.stackforge.kidtrack.platform.fleet.interfaces.rest;

import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Route;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.CreateRouteCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.DeleteRouteCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.UpdateRouteCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetRouteByIdQuery;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetRoutesQuery;
import com.stackforge.kidtrack.platform.fleet.domain.services.FleetCommandService;
import com.stackforge.kidtrack.platform.fleet.domain.services.FleetQueryService;
import com.stackforge.kidtrack.platform.fleet.interfaces.rest.resources.FleetResources.RouteRequestResource;
import com.stackforge.kidtrack.platform.fleet.interfaces.rest.resources.FleetResources.RouteResource;
import com.stackforge.kidtrack.platform.fleet.interfaces.rest.transform.FleetResourceAssembler;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
@Tag(name = "Routes", description = "School routes with their stops, driver, vehicle and students")
public class RoutesController {

    private final FleetCommandService commandService;
    private final FleetQueryService queryService;

    public RoutesController(FleetCommandService commandService, FleetQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the routes of an organization")
    public List<RouteResource> getRoutes(@RequestParam(required = false) String organizationId,
                                         @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        return queryService.handle(new GetRoutesQuery(scope)).stream()
                .map(FleetResourceAssembler::toResource).toList();
    }

    @GetMapping("/{routeId}")
    @Operation(summary = "Get a route by id")
    public RouteResource getRouteById(@PathVariable UUID routeId, @AuthenticationPrincipal Jwt jwt) {
        return FleetResourceAssembler.toResource(findInScope(routeId, jwt));
    }

    @PostMapping
    @Operation(summary = "Create a route")
    public ResponseEntity<RouteResource> createRoute(@Valid @RequestBody RouteRequestResource resource,
                                                     @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var organizationId = Caller.requireScope(caller, Ids.parseOrNull(resource.organizationId()));
        var route = commandService.handle(new CreateRouteCommand(organizationId, resource.name(), resource.type(),
                Ids.parseOrNull(resource.driverId()), resource.driverName(), Ids.parseOrNull(resource.vehicleId()),
                resource.vehiclePlate(), resource.studentIds(), resource.scheduledStartTime(), resource.status(),
                FleetResourceAssembler.toWaypointData(resource.waypoints())));
        return ResponseEntity.status(HttpStatus.CREATED).body(FleetResourceAssembler.toResource(route));
    }

    @PutMapping("/{routeId}")
    @Operation(summary = "Update a route",
            description = "Replaces the whole configuration; the pending trips of the route follow its new driver.")
    public RouteResource putRoute(@PathVariable UUID routeId, @Valid @RequestBody RouteRequestResource resource,
                                  @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(routeId, jwt);
        var route = commandService.handle(new UpdateRouteCommand(routeId, resource.name(), resource.type(),
                Ids.parseOrNull(resource.driverId()), resource.driverName(), Ids.parseOrNull(resource.vehicleId()),
                resource.vehiclePlate(), resource.studentIds(), resource.scheduledStartTime(), resource.status(),
                FleetResourceAssembler.toWaypointData(resource.waypoints())));
        return FleetResourceAssembler.toResource(route);
    }

    @DeleteMapping("/{routeId}")
    @Operation(summary = "Delete a route", description = "Also deletes the trips of the route.")
    public ResponseEntity<Void> deleteRoute(@PathVariable UUID routeId, @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(routeId, jwt);
        commandService.handle(new DeleteRouteCommand(routeId));
        return ResponseEntity.noContent().build();
    }

    private Route findInScope(UUID routeId, Jwt jwt) {
        var route = queryService.handle(new GetRouteByIdQuery(routeId))
                .orElseThrow(() -> new NotFoundException("Route was not found."));
        var caller = Caller.from(jwt);
        if (caller != null && caller.organizationId() != null
                && !caller.organizationId().equals(route.getOrganizationId())) {
            throw new AccessDeniedException("The route belongs to another organization.");
        }
        return route;
    }
}
