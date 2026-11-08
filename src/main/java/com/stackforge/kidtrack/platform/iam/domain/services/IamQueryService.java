package com.stackforge.kidtrack.platform.iam.domain.services;

import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.Organization;
import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.User;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetOrganizationByIdQuery;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetOrganizationHasAdminQuery;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetUserByIdQuery;
import com.stackforge.kidtrack.platform.iam.domain.model.queries.IamQueries.GetUsersQuery;
import java.util.List;
import java.util.Optional;

/** Read side of the IAM bounded context. */
public interface IamQueryService {

    Optional<User> handle(GetUserByIdQuery query);

    List<User> handle(GetUsersQuery query);

    Optional<Organization> handle(GetOrganizationByIdQuery query);

    boolean handle(GetOrganizationHasAdminQuery query);
}
