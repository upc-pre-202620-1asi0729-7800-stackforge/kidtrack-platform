package com.stackforge.kidtrack.platform.iam.interfaces.rest;

import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.User;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.UpdateUserCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetUserByIdQuery;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetUsersQuery;
import com.stackforge.kidtrack.platform.iam.domain.services.IamCommandService;
import com.stackforge.kidtrack.platform.iam.domain.services.IamQueryService;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.UpdateUserResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.UserResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.transform.IamResourceAssembler;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
import com.stackforge.kidtrack.platform.shared.interfaces.rest.Caller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Users of the platform")
public class UsersController {

    private final IamCommandService commandService;
    private final IamQueryService queryService;

    public UsersController(IamCommandService commandService, IamQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List users", description = "Lists the users of the caller's organization.")
    public List<UserResource> getUsers(@RequestParam(required = false) String organizationId,
                                       @AuthenticationPrincipal Jwt jwt) {
        var scope = Caller.scope(Caller.from(jwt), Ids.parseOrNull(organizationId));
        return queryService.handle(new GetUsersQuery(scope)).stream().map(IamResourceAssembler::toResource).toList();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user by id")
    public UserResource getUserById(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        var caller = Caller.from(jwt);
        var user = findUser(userId);
        if (!isSelf(caller, user) && !isSameOrganization(caller, user)) {
            throw new AccessDeniedException("The user belongs to another organization.");
        }
        return IamResourceAssembler.toResource(user);
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Update a user", description = "Partial update; absent fields are left untouched.")
    public UserResource patchUser(@PathVariable UUID userId, @RequestBody UpdateUserResource resource,
                                  @AuthenticationPrincipal Jwt jwt) {
        return update(userId, resource, jwt);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update a user")
    public UserResource putUser(@PathVariable UUID userId, @RequestBody UpdateUserResource resource,
                                @AuthenticationPrincipal Jwt jwt) {
        return update(userId, resource, jwt);
    }

    private UserResource update(UUID userId, UpdateUserResource resource, Jwt jwt) {
        var caller = Caller.from(jwt);
        var target = findUser(userId);
        var administrator = caller.isAdmin() && isSameOrganization(caller, target);
        if (!administrator && !isSelf(caller, target)) {
            throw new AccessDeniedException("You can only update your own profile.");
        }
        // A user editing their own profile cannot change the role or move to another organization.
        var organizationId = administrator || target.getOrganizationId() == null
                ? Ids.parseOrNull(resource.organizationId()) : null;
        var roleTier = administrator ? resource.roleTier() : null;
        var updated = commandService.handle(new UpdateUserCommand(userId, resource.firstName(), resource.lastName(),
                resource.email(), resource.phone(), roleTier, organizationId, resource.password()));
        return IamResourceAssembler.toResource(updated);
    }

    private User findUser(UUID userId) {
        return queryService.handle(new GetUserByIdQuery(userId))
                .orElseThrow(() -> new NotFoundException("User was not found."));
    }

    private static boolean isSelf(Caller caller, User user) {
        return caller != null && caller.userId().equals(user.getId());
    }

    private static boolean isSameOrganization(Caller caller, User user) {
        return caller != null && caller.organizationId() != null
                && Objects.equals(caller.organizationId(), user.getOrganizationId());
    }
}
