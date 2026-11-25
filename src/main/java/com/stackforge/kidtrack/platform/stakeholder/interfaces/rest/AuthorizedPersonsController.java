package com.stackforge.kidtrack.platform.stakeholder.interfaces.rest;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.RegisterAuthorizedPersonCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.RemoveAuthorizedPersonCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetAuthorizedPersonByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetAuthorizedPersonsQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.services.StakeholderCommandService;
import com.stackforge.kidtrack.platform.stakeholder.domain.services.StakeholderQueryService;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.AuthorizedPersonRequestResource;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.AuthorizedPersonResource;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.transform.StakeholderResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authorized-persons")
@Tag(name = "Authorized persons", description = "People registered to receive each student")
public class AuthorizedPersonsController {

    private final StakeholderCommandService commandService;
    private final StakeholderQueryService queryService;

    public AuthorizedPersonsController(StakeholderCommandService commandService,
                                       StakeholderQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the authorized persons", description = "Administrators and drivers.")
    public List<AuthorizedPersonResource> getAuthorizedPersons(
            @RequestParam(required = false) String organizationId, @RequestParam(required = false) String childId,
            @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAnyRole(caller, "ADMIN", "DRIVER");
        var scope = Caller.scope(caller, Ids.parseOrNull(organizationId));
        return queryService.handle(new GetAuthorizedPersonsQuery(scope, Ids.parseOrNull(childId))).stream()
                .map(StakeholderResourceAssembler::toResource).toList();
    }

    @PostMapping
    @Operation(summary = "Register an authorized person", description = "Administrators.")
    public ResponseEntity<AuthorizedPersonResource> register(
            @Valid @RequestBody AuthorizedPersonRequestResource resource, @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var organizationId = Caller.requireScope(caller, Ids.parseOrNull(resource.organizationId()));
        var person = commandService.handle(new RegisterAuthorizedPersonCommand(organizationId,
                Ids.parse(resource.childId(), "childId"), resource.fullName(), resource.documentNumber(),
                resource.relationship(), resource.phone()));
        return ResponseEntity.status(HttpStatus.CREATED).body(StakeholderResourceAssembler.toResource(person));
    }

    @DeleteMapping("/{authorizedPersonId}")
    @Operation(summary = "Remove an authorized person", description = "Administrators.")
    public ResponseEntity<Void> remove(@PathVariable UUID authorizedPersonId, @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var person = queryService.handle(new GetAuthorizedPersonByIdQuery(authorizedPersonId))
                .orElseThrow(() -> new NotFoundException("Authorized person was not found."));
        Caller.scope(caller, person.getOrganizationId());
        commandService.handle(new RemoveAuthorizedPersonCommand(authorizedPersonId));
        return ResponseEntity.noContent().build();
    }
}
