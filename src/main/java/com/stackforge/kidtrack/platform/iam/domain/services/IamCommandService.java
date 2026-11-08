package com.stackforge.kidtrack.platform.iam.domain.services;

import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.Organization;
import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.User;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.CreateOrganizationCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.SignInCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.SignUpCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.UpdateOrganizationCommand;
import com.stackforge.kidtrack.platform.iam.domain.model.commands.IamCommands.UpdateUserCommand;

/** Write side of the IAM bounded context. */
public interface IamCommandService {

    /** Result of a successful sign in: the user and the token issued for it. */
    record Authentication(User user, String token) {
    }

    User handle(SignUpCommand command);

    Authentication handle(SignInCommand command);

    User handle(UpdateUserCommand command);

    Organization handle(CreateOrganizationCommand command);

    Organization handle(UpdateOrganizationCommand command);
}
