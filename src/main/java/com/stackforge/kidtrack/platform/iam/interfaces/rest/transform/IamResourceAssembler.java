package com.stackforge.kidtrack.platform.iam.interfaces.rest.transform;

import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.Organization;
import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.User;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.AuthenticatedUserResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.OrganizationResource;
import com.stackforge.kidtrack.platform.iam.interfaces.rest.resources.IamResources.UserResource;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Dates;
import com.stackforge.kidtrack.platform.shared.domain.model.valueobjects.Ids;

/** Converts IAM aggregates into REST resources. */
public final class IamResourceAssembler {

    private IamResourceAssembler() {
    }

    public static UserResource toResource(User user) {
        return new UserResource(
                user.getId().toString(),
                user.getFullName().firstName(),
                user.getFullName().lastName(),
                user.getEmail().value(),
                user.getPhone() == null ? "" : user.getPhone(),
                user.getRole().name(),
                Ids.asText(user.getOrganizationId()));
    }

    public static AuthenticatedUserResource toResource(User user, String token) {
        var resource = toResource(user);
        return new AuthenticatedUserResource(resource.id(), resource.firstName(), resource.lastName(),
                resource.email(), resource.phone(), resource.roleTier(), resource.organizationId(), token);
    }

    public static OrganizationResource toResource(Organization organization) {
        return new OrganizationResource(
                organization.getId().toString(),
                organization.getName(),
                organization.getStatus().name(),
                Dates.format(organization.getCreatedAt()));
    }
}
