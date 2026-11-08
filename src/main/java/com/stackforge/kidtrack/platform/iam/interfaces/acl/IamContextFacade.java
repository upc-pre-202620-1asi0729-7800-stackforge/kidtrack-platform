package com.stackforge.kidtrack.platform.iam.interfaces.acl;

import java.util.Optional;
import java.util.UUID;

/** Anti-corruption facade that exposes the IAM data other bounded contexts need. */
public interface IamContextFacade {

    /** Full name of a user, when the user exists. */
    Optional<String> fullNameOfUser(UUID userId);
}
