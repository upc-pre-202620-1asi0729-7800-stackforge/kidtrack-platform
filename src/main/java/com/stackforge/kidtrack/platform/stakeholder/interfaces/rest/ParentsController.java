package com.stackforge.kidtrack.platform.stakeholder.interfaces.rest;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Parent;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.CreateParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.DeleteParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.UpdateParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetParentByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetParentsQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.services.StakeholderCommandService;
import com.stackforge.kidtrack.platform.stakeholder.domain.services.StakeholderQueryService;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.ParentRequestResource;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.ParentResource;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.transform.StakeholderResourceAssembler;
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
@RequestMapping("/api/v1/parents")
@Tag(name = "Parents", description = "Parents (guardians) of the students")
public class ParentsController {

    private final StakeholderCommandService commandService;
    private final StakeholderQueryService queryService;

    public ParentsController(StakeholderCommandService commandService, StakeholderQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the parents of an organization")
    public List<ParentResource> getParents(@RequestParam(required = false) String organizationId,
                                           @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        return queryService.handle(new GetParentsQuery(scope)).stream()
                .map(StakeholderResourceAssembler::toResource).toList();
    }

    @GetMapping("/{parentId}")
    @Operation(summary = "Get a parent by id")
    public ParentResource getParentById(@PathVariable UUID parentId, @AuthenticationPrincipal Jwt jwt) {
        return StakeholderResourceAssembler.toResource(findInScope(parentId, jwt));
    }

    @PostMapping
    @Operation(summary = "Create a parent")
    public ResponseEntity<ParentResource> createParent(@Valid @RequestBody ParentRequestResource resource,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var organizationId = Caller.requireScope(caller, Ids.parseOrNull(resource.organizationId()));
        var parent = commandService.handle(new CreateParentCommand(organizationId, null, resource.name(),
                resource.email(), resource.phone(), resource.status()));
        return ResponseEntity.status(HttpStatus.CREATED).body(StakeholderResourceAssembler.toResource(parent));
    }

    @PutMapping("/{parentId}")
    @Operation(summary = "Update a parent")
    public ParentResource putParent(@PathVariable UUID parentId, @RequestBody ParentRequestResource resource,
                                    @AuthenticationPrincipal Jwt jwt) {
        return update(parentId, resource, jwt);
    }

    @PatchMapping("/{parentId}")
    @Operation(summary = "Partially update a parent")
    public ParentResource patchParent(@PathVariable UUID parentId, @RequestBody ParentRequestResource resource,
                                      @AuthenticationPrincipal Jwt jwt) {
        return update(parentId, resource, jwt);
    }

    @DeleteMapping("/{parentId}")
    @Operation(summary = "Delete a parent", description = "The students of the parent are kept, without a parent.")
    public ResponseEntity<Void> deleteParent(@PathVariable UUID parentId, @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(parentId, jwt);
        commandService.handle(new DeleteParentCommand(parentId));
        return ResponseEntity.noContent().build();
    }

    private ParentResource update(UUID parentId, ParentRequestResource resource, Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(parentId, jwt);
        var parent = commandService.handle(new UpdateParentCommand(parentId, resource.name(), resource.email(),
                resource.phone(), resource.status()));
        return StakeholderResourceAssembler.toResource(parent);
    }

    private Parent findInScope(UUID parentId, Jwt jwt) {
        var parent = queryService.handle(new GetParentByIdQuery(parentId))
                .orElseThrow(() -> new NotFoundException("Parent was not found."));
        var caller = Caller.from(jwt);
        if (caller != null && caller.organizationId() != null
                && !caller.organizationId().equals(parent.getOrganizationId())) {
            throw new AccessDeniedException("The parent belongs to another organization.");
        }
        return parent;
    }
}
