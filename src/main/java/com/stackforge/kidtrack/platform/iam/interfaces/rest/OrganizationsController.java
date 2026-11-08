package com.stackforge.kidtrack.platform.iam.interfaces.rest;

import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.CreateOrganizationCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.UpdateOrganizationCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetOrganizationByIdQuery;
import com.stackforge.kidtrack.platform.iam.domain.services.IamCommandService;
import com.stackforge.kidtrack.platform.iam.domain.services.IamQueryService;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.OrganizationRequestResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.OrganizationResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.UpdateOrganizationResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.transform.IamResourceAssembler;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Transport companies (tenants)")
public class OrganizationsController {

    private final IamCommandService commandService;
    private final IamQueryService queryService;

    public OrganizationsController(IamCommandService commandService, IamQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @Operation(summary = "Create an organization",
            description = "Public: the organization is created before its first administrator registers.")
    public ResponseEntity<OrganizationResource> createOrganization(
            @Valid @RequestBody OrganizationRequestResource resource) {
        var organization = commandService.handle(new CreateOrganizationCommand(resource.name(), resource.status()));
        return ResponseEntity.status(HttpStatus.CREATED).body(IamResourceAssembler.toResource(organization));
    }

    @GetMapping("/{organizationId}")
    @Operation(summary = "Get an organization by id")
    public OrganizationResource getOrganizationById(@PathVariable UUID organizationId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        Caller.scope(Caller.from(jwt), organizationId);
        var organization = queryService.handle(new GetOrganizationByIdQuery(organizationId))
                .orElseThrow(() -> new NotFoundException("Organization was not found."));
        return IamResourceAssembler.toResource(organization);
    }

    @PatchMapping("/{organizationId}")
    @Operation(summary = "Update an organization")
    public OrganizationResource patchOrganization(@PathVariable UUID organizationId,
                                                  @RequestBody UpdateOrganizationResource resource,
                                                  @AuthenticationPrincipal Jwt jwt) {
        return update(organizationId, resource, jwt);
    }

    @PutMapping("/{organizationId}")
    @Operation(summary = "Update an organization")
    public OrganizationResource putOrganization(@PathVariable UUID organizationId,
                                                @RequestBody UpdateOrganizationResource resource,
                                                @AuthenticationPrincipal Jwt jwt) {
        return update(organizationId, resource, jwt);
    }

    private OrganizationResource update(UUID organizationId, UpdateOrganizationResource resource, Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        if (!organizationId.equals(caller.organizationId())) {
            throw new AccessDeniedException("The organization belongs to another administrator.");
        }
        var organization = commandService.handle(
                new UpdateOrganizationCommand(organizationId, resource.name(), resource.status()));
        return IamResourceAssembler.toResource(organization);
    }
}
