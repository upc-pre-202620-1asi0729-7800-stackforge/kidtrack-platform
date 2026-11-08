package com.stackforge.kidtrack.platform.iam.interfaces.rest;

import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.SignInCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.SignUpCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetOrganizationHasAdminQuery;
import com.stackforge.kidtrack.platform.iam.domain.model.valueobjects.RoleTier;
import com.stackforge.kidtrack.platform.iam.domain.services.IamCommandService;
import com.stackforge.kidtrack.platform.iam.domain.services.IamQueryService;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.AuthenticatedUserResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.SignInResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.SignUpResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.UserResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.transform.IamResourceAssembler;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sign in and sign up endpoints. */
@RestController
@RequestMapping("/api/v1/authentication")
@Tag(name = "Authentication", description = "Sign in and sign up")
public class AuthenticationController {

    private final IamCommandService commandService;
    private final IamQueryService queryService;

    public AuthenticationController(IamCommandService commandService, IamQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Sign in", description = "Validates the credentials and returns the user with a JWT.")
    public ResponseEntity<AuthenticatedUserResource> signIn(@Valid @RequestBody SignInResource resource) {
        var authentication = commandService.handle(new SignInCommand(resource.email(), resource.password()));
        return ResponseEntity.ok(IamResourceAssembler.toResource(authentication.user(), authentication.token()));
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Sign up",
            description = "Registers a user. The first administrator of an organization can register anonymously; "
                    + "drivers, parents and further administrators must be registered by an administrator.")
    public ResponseEntity<UserResource> signUp(@Valid @RequestBody SignUpResource resource,
                                               @AuthenticationPrincipal Jwt jwt) {
        var role = RoleTier.from(resource.roleTier());
        var organizationId = Ids.parseOrNull(resource.organizationId());
        authorizeSignUp(role, organizationId, Caller.from(jwt));
        var user = commandService.handle(new SignUpCommand(resource.firstName(), resource.lastName(),
                resource.email(), resource.password(), role.name(), organizationId));
        return ResponseEntity.status(HttpStatus.CREATED).body(IamResourceAssembler.toResource(user));
    }

    private void authorizeSignUp(RoleTier role, UUID organizationId, Caller caller) {
        var adminOfSameOrganization = caller != null && caller.isAdmin()
                && (organizationId == null || caller.organizationId() == null
                || caller.organizationId().equals(organizationId));
        if (adminOfSameOrganization) {
            return;
        }
        var firstAdminOfOrganization = role == RoleTier.ADMIN
                && (organizationId == null || !queryService.handle(new GetOrganizationHasAdminQuery(organizationId)));
        if (!firstAdminOfOrganization) {
            throw new AccessDeniedException("Only an administrator of the organization can register this user.");
        }
    }
}
