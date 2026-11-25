package com.stackforge.kidtrack.platform.trip.interfaces.rest;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import com.stackforge.kidtrack.platform.trip.domain.model.commands.TripCommands.RecordHandoverCommand;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetHandoversQuery;
import com.stackforge.kidtrack.platform.trip.domain.model.queries.TripQueries.GetTripByIdQuery;
import com.stackforge.kidtrack.platform.trip.domain.services.TripCommandService;
import com.stackforge.kidtrack.platform.trip.domain.services.TripQueryService;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.resources.TripResources.HandoverRequestResource;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.resources.TripResources.HandoverResource;
import com.stackforge.kidtrack.platform.trip.interfaces.rest.transform.TripResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/handovers")
@Tag(name = "Handovers", description = "Who received each student during a trip")
public class HandoversController {

    private final TripCommandService commandService;
    private final TripQueryService queryService;

    public HandoversController(TripCommandService commandService, TripQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the handovers", description = "Oldest first. Optional filter: tripId.")
    public List<HandoverResource> getHandovers(@RequestParam(required = false) String organizationId,
                                               @RequestParam(required = false) String tripId,
                                               @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        return queryService.handle(new GetHandoversQuery(scope, Ids.parseOrNull(tripId))).stream()
                .map(TripResourceAssembler::toResource).toList();
    }

    @PostMapping
    @Operation(summary = "Hand a student over", description = "The driver of the trip (or an administrator) records "
            + "which registered person received the student, or that nobody registered was there.")
    public ResponseEntity<HandoverResource> record(@Valid @RequestBody HandoverRequestResource resource,
                                                   @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAnyRole(caller, "ADMIN", "DRIVER");
        var tripId = Ids.parse(resource.tripId(), "tripId");
        var trip = queryService.handle(new GetTripByIdQuery(tripId))
                .orElseThrow(() -> new NotFoundException("Trip was not found."));
        Caller.scope(caller, trip.getOrganizationId());
        if (!caller.isAdmin() && !caller.userId().equals(trip.getDriverId())) {
            throw new AccessDeniedException("Only the driver assigned to the trip can hand students over.");
        }
        var handover = commandService.handle(new RecordHandoverCommand(tripId, Ids.parse(resource.childId(), "childId"),
                Ids.parseOrNull(resource.authorizedPersonId()), resource.receiverName(),
                jwt.getClaimAsString("name")));
        return ResponseEntity.status(HttpStatus.CREATED).body(TripResourceAssembler.toResource(handover));
    }
}
