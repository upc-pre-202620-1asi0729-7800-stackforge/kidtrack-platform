package com.stackforge.kidtrack.platform.trip.interfaces.rest;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import com.stackforge.kidtrack.platform.trip.domain.model.aggregates.Incident;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.PatchIncidentCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.ReportIncidentCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetIncidentByIdQuery;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetIncidentsQuery;
import com.stackforge.kidtrack.platform.trip.domain.services.TripCommandService;
import com.stackforge.kidtrack.platform.trip.domain.services.TripQueryService;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.resources.TripResources.IncidentPatchResource;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.resources.TripResources.IncidentRequestResource;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.resources.TripResources.IncidentResource;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.transform.TripResourceAssembler;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Incidents reported by the drivers during the trips")
public class IncidentsController {

    private final TripCommandService commandService;
    private final TripQueryService queryService;

    public IncidentsController(TripCommandService commandService, TripQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the incidents", description = "Newest first. Optional filters: tripId and status.")
    public List<IncidentResource> getIncidents(@RequestParam(required = false) String organizationId,
                                               @RequestParam(required = false) String tripId,
                                               @RequestParam(required = false) String status,
                                               @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        return queryService.handle(new GetIncidentsQuery(scope, Ids.parseOrNull(tripId), status)).stream()
                .map(TripResourceAssembler::toResource).toList();
    }

    @GetMapping("/{incidentId}")
    @Operation(summary = "Get an incident by id")
    public IncidentResource getIncidentById(@PathVariable UUID incidentId, @AuthenticationPrincipal Jwt jwt) {
        return TripResourceAssembler.toResource(findInScope(incidentId, jwt));
    }

    @PostMapping
    @Operation(summary = "Report an incident", description = "Administrators and drivers.")
    public ResponseEntity<IncidentResource> reportIncident(@Valid @RequestBody IncidentRequestResource resource,
                                                           @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAnyRole(caller, "ADMIN", "DRIVER");
        var organizationId = Caller.requireScope(caller, Ids.parseOrNull(resource.organizationId()));
        var reportedBy = resource.reportedBy() == null || resource.reportedBy().isBlank()
                ? jwt.getClaimAsString("name") : resource.reportedBy();
        var incident = commandService.handle(new ReportIncidentCommand(organizationId,
                Ids.parseOrNull(resource.tripId()), Ids.parseOrNull(resource.routeId()), resource.type(),
                resource.severity(), resource.description(), reportedBy, resource.timestamp()));
        return ResponseEntity.status(HttpStatus.CREATED).body(TripResourceAssembler.toResource(incident));
    }

    @PatchMapping("/{incidentId}")
    @Operation(summary = "Update an incident", description = "Marks it as resolved (or open again).")
    public IncidentResource patchIncident(@PathVariable UUID incidentId, @RequestBody IncidentPatchResource resource,
                                          @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(incidentId, jwt);
        return TripResourceAssembler.toResource(
                commandService.handle(new PatchIncidentCommand(incidentId, resource.status())));
    }

    private Incident findInScope(UUID incidentId, Jwt jwt) {
        var incident = queryService.handle(new GetIncidentByIdQuery(incidentId))
                .orElseThrow(() -> new NotFoundException("Incident was not found."));
        var caller = Caller.from(jwt);
        if (caller != null && caller.organizationId() != null
                && !caller.organizationId().equals(incident.getOrganizationId())) {
            throw new AccessDeniedException("The incident belongs to another organization.");
        }
        return incident;
    }
}
