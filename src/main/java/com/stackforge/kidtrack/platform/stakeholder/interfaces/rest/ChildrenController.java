package com.stackforge.kidtrack.platform.stakeholder.interfaces.rest;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Child;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.CreateChildCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.DeleteChildCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.UpdateChildCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetChildByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetChildrenQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.services.StakeholderCommandService;
import com.stackforge.kidtrack.platform.stakeholder.domain.services.StakeholderQueryService;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.ChildPatchResource;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.ChildRequestResource;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.ChildResource;
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
@RequestMapping("/api/v1/children")
@Tag(name = "Children", description = "Students that use the school transport")
public class ChildrenController {

    private final StakeholderCommandService commandService;
    private final StakeholderQueryService queryService;

    public ChildrenController(StakeholderCommandService commandService, StakeholderQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the students", description = "Students of the organization, optionally of one parent.")
    public List<ChildResource> getChildren(@RequestParam(required = false) String organizationId,
                                           @RequestParam(required = false) String parentId,
                                           @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        return queryService.handle(new GetChildrenQuery(scope, Ids.parseOrNull(parentId))).stream()
                .map(StakeholderResourceAssembler::toResource).toList();
    }

    @GetMapping("/{childId}")
    @Operation(summary = "Get a student by id")
    public ChildResource getChildById(@PathVariable UUID childId, @AuthenticationPrincipal Jwt jwt) {
        return StakeholderResourceAssembler.toResource(findInScope(childId, jwt));
    }

    @PostMapping
    @Operation(summary = "Create a student")
    public ResponseEntity<ChildResource> createChild(@Valid @RequestBody ChildRequestResource resource,
                                                     @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var organizationId = Caller.requireScope(caller, Ids.parseOrNull(resource.organizationId()));
        var child = commandService.handle(new CreateChildCommand(organizationId, Ids.parseOrNull(resource.parentId()),
                resource.name(), resource.grade(), resource.status(), resource.boardingStatus(),
                resource.hasPhoto()));
        return ResponseEntity.status(HttpStatus.CREATED).body(StakeholderResourceAssembler.toResource(child));
    }

    @PutMapping("/{childId}")
    @Operation(summary = "Update a student", description = "Full update; a missing parent detaches the student.")
    public ChildResource putChild(@PathVariable UUID childId, @Valid @RequestBody ChildRequestResource resource,
                                  @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(childId, jwt);
        var parentId = Ids.parseOrNull(resource.parentId());
        var child = commandService.handle(new UpdateChildCommand(childId, parentId, parentId == null,
                resource.name(), resource.grade(), resource.status(), resource.boardingStatus(),
                resource.hasPhoto()));
        return StakeholderResourceAssembler.toResource(child);
    }

    @PatchMapping("/{childId}")
    @Operation(summary = "Partially update a student",
            description = "Administrators and drivers; drivers use it to record the boarding status.")
    public ChildResource patchChild(@PathVariable UUID childId, @RequestBody ChildPatchResource resource,
                                    @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAnyRole(Caller.from(jwt), "ADMIN", "DRIVER");
        findInScope(childId, jwt);
        var child = commandService.handle(new UpdateChildCommand(childId, Ids.parseOrNull(resource.parentId()),
                false, resource.name(), resource.grade(), resource.status(), resource.boardingStatus(),
                resource.hasPhoto()));
        return StakeholderResourceAssembler.toResource(child);
    }

    @DeleteMapping("/{childId}")
    @Operation(summary = "Delete a student")
    public ResponseEntity<Void> deleteChild(@PathVariable UUID childId, @AuthenticationPrincipal Jwt jwt) {
        Caller.requireAdmin(Caller.from(jwt));
        findInScope(childId, jwt);
        commandService.handle(new DeleteChildCommand(childId));
        return ResponseEntity.noContent().build();
    }

    private Child findInScope(UUID childId, Jwt jwt) {
        var child = queryService.handle(new GetChildByIdQuery(childId))
                .orElseThrow(() -> new NotFoundException("Student was not found."));
        var caller = Caller.from(jwt);
        if (caller != null && caller.organizationId() != null
                && !caller.organizationId().equals(child.getOrganizationId())) {
            throw new AccessDeniedException("The student belongs to another organization.");
        }
        return child;
    }
}
