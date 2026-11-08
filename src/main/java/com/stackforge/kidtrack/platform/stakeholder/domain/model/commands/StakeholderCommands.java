package com.stackforge.kidtrack.platform.stakeholder.domain.model.commands;

import java.util.UUID;

/** Commands handled by the Stakeholder bounded context. Nullable fields of the update commands are left untouched. */
public final class StakeholderCommands {

    private StakeholderCommands() {
    }

    public record CreateParentCommand(UUID organizationId, UUID userId, String name, String email, String phone,
                                      Boolean active) {
    }

    public record UpdateParentCommand(UUID parentId, String name, String email, String phone, Boolean active) {
    }

    public record DeleteParentCommand(UUID parentId) {
    }

    public record CreateChildCommand(UUID organizationId, UUID parentId, String name, String grade, Boolean active,
                                     String boardingStatus, Boolean hasPhoto) {
    }

    /** {@code clearParent} detaches the child from its parent (a full replacement without parent). */
    public record UpdateChildCommand(UUID childId, UUID parentId, boolean clearParent, String name, String grade,
                                     Boolean active, String boardingStatus, Boolean hasPhoto) {
    }

    public record DeleteChildCommand(UUID childId) {
    }

    public record CreateDriverCommand(UUID organizationId, UUID userId, String firstName, String lastName,
                                      String phone, String licenseNumber, UUID vehicleId, Boolean active) {
    }

    /** {@code clearVehicle} removes the vehicle of the driver (a full replacement without vehicle). */
    public record UpdateDriverCommand(UUID driverId, UUID userId, String firstName, String lastName, String phone,
                                      String licenseNumber, UUID vehicleId, boolean clearVehicle, Boolean active) {
    }

    public record DeleteDriverCommand(UUID driverId) {
    }
}
