package com.stackforge.kidtrack.platform.fleet.interfaces.rest;

import com.stackforge.kidtrack.platform.fleet.domain.model.aggregates.Vehicle;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.CreateVehicleCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.DeleteVehicleCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.commands.FleetCommands.UpdateVehicleCommand;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetVehicleByIdQuery;
import com.stackforge.kidtrack.platform.fleet.domain.model.queries.FleetQueries.GetVehiclesQuery;
import com.stackforge.kidtrack.platform.fleet.domain.services.FleetCommandService;
import com.stackforge.kidtrack.platform.fleet.domain.services.FleetQueryService;
import com.stackforge.kidtrack.platform.fleet.interfaces.rest.resources.FleetResources.VehicleRequestResource;
import com.stackforge.kidtrack.platform.fleet.interfaces.rest.resources.FleetResources.VehicleResource;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Vehicles of the fleet")
public class VehiclesController {

    private final FleetCommandService commandService;
    private final FleetQueryService queryService;

    public VehiclesController(FleetCommandService commandService, FleetQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the vehicles of an organization")
    public List<VehicleResource> getVehicles(@RequestParam(required = false) String organizationId,
                                             @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        return queryService.handle(new GetVehiclesQuery(scope)).stream()
                .map(FleetResourceAssembler::toResource).toList();
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Get a vehicle by id")
    public VehicleResource getVehicleById(@PathVariable UUID vehicleId, @AuthenticationPrincipal Jwt jwt) {
        return FleetResourceAssembler.toResource(findInScope(vehicleId, jwt));
    }

    @PostMapping
    @Operation(summary = "Create a vehicle")
    public ResponseEntity<VehicleResource> createVehicle(@Valid @RequestBody VehicleRequestResource resource,
                                                         @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var organizationId = Caller.requireScope(caller, Ids.parseOrNull(resource.organizationId()));
        var vehicle = commandService.handle(new CreateVehicleCommand(organizationId, resource.plate(),
                resource.model(), resource.capacity(), resource.status()));
        return ResponseEntity.status(HttpStatus.CREATED).body(FleetResourceAssembler.toResource(vehicle));
    }

    @PutMapping("/{vehicleId}")
    @Operation(summary = "Update a vehicle")
    public VehicleResource putVehicle(@PathVariable UUID vehicleId, @Valid @RequestBody VehicleRequestResource resource,
                                      @AuthenticationPrincipal Jwt jwt) {
        return update(vehicleId, resource, jwt);
    }

    @PatchMapping("/{vehicleId}")
    @Operation(summary = "Partially update a vehicle")
    public VehicleResource patchVehicle(@PathVariable UUID vehicleId,
                                        @RequestBody VehicleRequestResource resource,
                                        @AuthenticationPrincipal Jwt jwt) {
        return update(vehicleId, resource, jwt);
    }

    @DeleteMapping("/{vehicleId}")
    @Operation(summary = "Delete a vehicle", description = "The routes that used it are left without vehicle.")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID vehicleId, @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(vehicleId, jwt);
        commandService.handle(new DeleteVehicleCommand(vehicleId));
        return ResponseEntity.noContent().build();
    }

    private VehicleResource update(UUID vehicleId, VehicleRequestResource resource, Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(vehicleId, jwt);
        var vehicle = commandService.handle(new UpdateVehicleCommand(vehicleId, resource.plate(), resource.model(),
                resource.capacity() > 0 ? resource.capacity() : null, resource.status()));
        return FleetResourceAssembler.toResource(vehicle);
    }

    private Vehicle findInScope(UUID vehicleId, Jwt jwt) {
        var vehicle = queryService.handle(new GetVehicleByIdQuery(vehicleId))
                .orElseThrow(() -> new NotFoundException("Vehicle was not found."));
        var caller = Caller.from(jwt);
        if (caller != null && caller.organizationId() != null
                && !caller.organizationId().equals(vehicle.getOrganizationId())) {
            throw new AccessDeniedException("The vehicle belongs to another organization.");
        }
        return vehicle;
    }
}
