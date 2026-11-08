package com.stackforge.kidtrack.platform.iam.domain.model.queries;

import java.util.UUID;

/** Queries handled by the IAM bounded context. */
public final class IamQueries {

    private IamQueries() {
    }

    public record GetUserByIdQuery(UUID userId) {
    }

    /** Lists the users, optionally restricted to an organization. */
    public record GetUsersQuery(UUID organizationId) {
    }

    public record GetOrganizationByIdQuery(UUID organizationId) {
    }

    public record GetOrganizationHasAdminQuery(UUID organizationId) {
    }
}
