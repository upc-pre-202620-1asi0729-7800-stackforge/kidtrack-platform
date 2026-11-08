package com.stackforge.kidtrack.platform.stakeholder.interfaces.rest;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Driver;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.aggregates.Parent;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.CreateDriverCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.CreateParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.DeleteDriverCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.DeleteParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.UpdateDriverCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.commands.StakeholderCommands.UpdateParentCommand;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetDriverByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetDriversQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetParentByIdQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.model.queries.StakeholderQueries.GetParentsQuery;
import com.stackforge.kidtrack.platform.stakeholder.domain.services.StakeholderCommandService;
import com.stackforge.kidtrack.platform.stakeholder.domain.services.StakeholderQueryService;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.ProfileRequestResource;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.resources.StakeholderResources.ProfileResource;
import com.stackforge.kidtrack.platform.stakeholder.interfaces.rest.transform.StakeholderResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

/**
 * Person profiles of the organization: drivers ({@code role=driver}) and parents ({@code role=parent}).
 * The web application manages its drivers through this resource.
 */
@RestController
@RequestMapping("/api/v1/profiles")
@Tag(name = "Profiles", description = "Driver and parent profiles")
public class ProfilesController {

    private final StakeholderCommandService commandService;
    private final StakeholderQueryService queryService;

    public ProfilesController(StakeholderCommandService commandService, StakeholderQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the profiles", description = "Filter by organization and by role (driver or parent).")
    public List<ProfileResource> getProfiles(@RequestParam(required = false) String organizationId,
                                             @RequestParam(required = false) String role,
                                             @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        var profiles = new ArrayList<ProfileResource>();
        if (role == null || role.isBlank() || StakeholderResourceAssembler.ROLE_DRIVER.equalsIgnoreCase(role)) {
            queryService.handle(new GetDriversQuery(scope)).stream()
                    .map(StakeholderResourceAssembler::toProfile).forEach(profiles::add);
        }
        if (role == null || role.isBlank() || StakeholderResourceAssembler.ROLE_PARENT.equalsIgnoreCase(role)) {
            queryService.handle(new GetParentsQuery(scope)).stream()
                    .map(StakeholderResourceAssembler::toProfile).forEach(profiles::add);
        }
        return profiles;
    }

    @GetMapping("/{profileId}")
    @Operation(summary = "Get a profile by id")
    public ProfileResource getProfileById(@PathVariable UUID profileId, @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        var driver = queryService.handle(new GetDriverByIdQuery(profileId));
        if (driver.isPresent()) {
            checkScope(caller, driver.get().getOrganizationId());
            return StakeholderResourceAssembler.toProfile(driver.get());
        }
        var parent = findParent(profileId);
        checkScope(caller, parent.getOrganizationId());
        return StakeholderResourceAssembler.toProfile(parent);
    }

    @PostMapping
    @Operation(summary = "Create a profile", description = "Creates a driver (default) or a parent.")
    public ResponseEntity<ProfileResource> createProfile(@RequestBody ProfileRequestResource resource,
                                                         @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var organizationId = Caller.requireScope(caller, Ids.parseOrNull(resource.organizationId()));
        ProfileResource created;
        if (StakeholderResourceAssembler.ROLE_PARENT.equalsIgnoreCase(resource.role())) {
            var parent = commandService.handle(new CreateParentCommand(organizationId,
                    Ids.parseOrNull(resource.userId()), fullName(resource), null, resource.phone(),
                    isActive(resource.status())));
            created = StakeholderResourceAssembler.toProfile(parent);
        } else {
            var driver = commandService.handle(new CreateDriverCommand(organizationId,
                    Ids.parseOrNull(resource.userId()), resource.firstName(), resource.lastName(), resource.phone(),
                    resource.license(), Ids.parseOrNull(resource.vehicleId()), isActive(resource.status())));
            created = StakeholderResourceAssembler.toProfile(driver);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{profileId}")
    @Operation(summary = "Update a profile")
    public ProfileResource putProfile(@PathVariable UUID profileId, @RequestBody ProfileRequestResource resource,
                                      @AuthenticationPrincipal Jwt jwt) {
        return update(profileId, resource, true, jwt);
    }

    @PatchMapping("/{profileId}")
    @Operation(summary = "Partially update a profile")
    public ProfileResource patchProfile(@PathVariable UUID profileId, @RequestBody ProfileRequestResource resource,
                                        @AuthenticationPrincipal Jwt jwt) {
        return update(profileId, resource, false, jwt);
    }

    @DeleteMapping("/{profileId}")
    @Operation(summary = "Delete a profile")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID profileId, @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        Optional<Driver> driver = queryService.handle(new GetDriverByIdQuery(profileId));
        if (driver.isPresent()) {
            checkScope(caller, driver.get().getOrganizationId());
            commandService.handle(new DeleteDriverCommand(profileId));
        } else {
            checkScope(caller, findParent(profileId).getOrganizationId());
            commandService.handle(new DeleteParentCommand(profileId));
        }
        return ResponseEntity.noContent().build();
    }

    private ProfileResource update(UUID profileId, ProfileRequestResource resource, boolean replace, Jwt jwt) {
        var caller = Caller.from(jwt);
        Caller.requireAdmin(caller);
        var driver = queryService.handle(new GetDriverByIdQuery(profileId));
        if (driver.isPresent()) {
            checkScope(caller, driver.get().getOrganizationId());
            var vehicleId = Ids.parseOrNull(resource.vehicleId());
            var updated = commandService.handle(new UpdateDriverCommand(profileId, Ids.parseOrNull(resource.userId()),
                    resource.firstName(), resource.lastName(), resource.phone(), resource.license(), vehicleId,
                    replace && vehicleId == null, resource.status() == null ? null : isActive(resource.status())));
            return StakeholderResourceAssembler.toProfile(updated);
        }
        var parent = findParent(profileId);
        checkScope(caller, parent.getOrganizationId());
        var updated = commandService.handle(new UpdateParentCommand(profileId,
                resource.firstName() == null && resource.lastName() == null ? null : fullName(resource),
                null, resource.phone(), resource.status() == null ? null : isActive(resource.status())));
        return StakeholderResourceAssembler.toProfile(updated);
    }

    private Parent findParent(UUID profileId) {
        return queryService.handle(new GetParentByIdQuery(profileId))
                .orElseThrow(() -> new NotFoundException("Profile was not found."));
    }

    private static void checkScope(Caller caller, UUID organizationId) {
        if (caller != null && caller.organizationId() != null && !caller.organizationId().equals(organizationId)) {
            throw new AccessDeniedException("The profile belongs to another organization.");
        }
    }

    private static String fullName(ProfileRequestResource resource) {
        return ((resource.firstName() == null ? "" : resource.firstName()) + " "
                + (resource.lastName() == null ? "" : resource.lastName())).trim();
    }

    private static boolean isActive(String status) {
        return status == null || !"INACTIVE".equalsIgnoreCase(status.trim());
    }
}
