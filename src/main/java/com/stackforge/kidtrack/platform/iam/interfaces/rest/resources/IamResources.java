package com.stackforge.kidtrack.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

/** REST resources of the IAM bounded context. Identifiers travel as text, like in the web application. */
public final class IamResources {

    private IamResources() {
    }

    public record SignInResource(@NotBlank String email, @NotBlank String password) {
    }

    public record SignUpResource(@NotBlank String firstName, String lastName, @NotBlank String email,
                                 @NotBlank String password, @NotBlank String roleTier, String organizationId) {
    }

    /** Partial update of a user; absent fields are left untouched. */
    public record UpdateUserResource(String firstName, String lastName, String email, String phone,
                                     String roleTier, String organizationId, String password) {
    }

    public record UserResource(String id, String firstName, String lastName, String email, String phone,
                               String roleTier, String organizationId) {
    }

    /** A user plus the access token issued at sign in. */
    public record AuthenticatedUserResource(String id, String firstName, String lastName, String email, String phone,
                                            String roleTier, String organizationId, String token) {
    }

    public record OrganizationRequestResource(@NotBlank String name, String status) {
    }

    public record UpdateOrganizationResource(String name, String status) {
    }

    public record OrganizationResource(String id, String name, String status, String createdAt) {
    }
}
