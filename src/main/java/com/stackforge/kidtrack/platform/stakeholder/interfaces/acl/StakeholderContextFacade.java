package com.stackforge.kidtrack.platform.stakeholder.interfaces.acl;

import java.util.Optional;
import java.util.UUID;

/** Anti-corruption facade that exposes the stakeholder data other bounded contexts need. */
public interface StakeholderContextFacade {

    /** Full name of the driver whose sign in account (or driver profile) has the given identifier. */
    Optional<String> driverFullName(UUID userOrDriverId);

    /** Name of a student. */
    Optional<String> childName(UUID childId);
}
