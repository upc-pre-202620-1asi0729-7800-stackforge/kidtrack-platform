package com.stackforge.kidtrack.platform.stakeholder.interfaces.acl;

import java.util.Optional;
import java.util.UUID;

/** Anti-corruption facade that exposes the stakeholder data other bounded contexts need. */
public interface StakeholderContextFacade {

    /** A student and the parent that must be informed about them. */
    record ChildContact(UUID childId, String name, UUID parentId) {
    }

    /** A person registered to receive a student. */
    record Receiver(UUID id, UUID childId, String fullName, String relationship) {
    }

    Optional<ChildContact> childContact(UUID childId);

    Optional<Receiver> authorizedPerson(UUID authorizedPersonId);

    /** Full name of the driver whose sign in account (or driver profile) has the given identifier. */
    Optional<String> driverFullName(UUID userOrDriverId);

    /** Name of a student. */
    Optional<String> childName(UUID childId);
}
