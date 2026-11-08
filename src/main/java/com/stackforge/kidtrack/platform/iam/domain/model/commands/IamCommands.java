package com.stackforge.kidtrack.platform.iam.domain.model.commands;

import java.util.UUID;

/** Commands handled by the IAM bounded context. */
public final class IamCommands {

    private IamCommands() {
    }

    public record SignUpCommand(String firstName, String lastName, String email, String password,
                                String roleTier, UUID organizationId) {
    }

    public record SignInCommand(String email, String password) {
    }

    /** Partial update of a user; {@code null} fields are left untouched. */
    public record UpdateUserCommand(UUID userId, String firstName, String lastName, String email, String phone,
                                    String roleTier, UUID organizationId, String password) {
    }

    public record CreateOrganizationCommand(String name, String status) {
    }

    public record UpdateOrganizationCommand(UUID organizationId, String name, String status) {
    }
}
